import { Component, HostListener, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

type SectionId = 'dashboard' | 'employees' | 'departments' | 'orgchart' | 'positions' | 'attendance' | 'vacations' | 'documents' | 'reports' | 'settings';
type EmployeeProfileTab = 'personal' | 'job' | 'attendance' | 'timeoff' | 'performance' | 'documents' | 'onboarding' | 'tasks' | 'more';
type PhotoAdjustmentField = 'x' | 'y' | 'zoom';

interface PhotoAdjustment {
  x: number;
  y: number;
  zoom: number;
}

interface Employee {
  id: number;
  name: string;
  email: string;
  phone: string;
  department: string;
  position: string;
  employmentType: string;
  status: 'active' | 'remote' | 'leave';
  statusLabel: string;
  hireDate: string;
  manager: string;
  location: string;
  salaryGrade: string;
  photoUrl?: string;
  zktecoPin?: string;
  adUsername?: string;
  rootDepartment?: string;
}

interface LiveAttendanceEmployee {
  pin: string;
  name: string;
  lastName: string;
  eventPointName: string;
  areaName: string;
  eventTime: string;
  departmentName: string;
  rootDepartment: string;
  photoPath: string;
  status: 'INSIDE' | 'OUTSIDE' | 'UNKNOWN';
}

interface AttendanceHistory {
  pin: string;
  name: string;
  lastName: string;
  eventPointName: string;
  areaName: string;
  eventTime: string;
  status: 'INSIDE' | 'OUTSIDE' | 'UNKNOWN';
}

interface AttendanceDepartmentGroup {
  rootDepartment: string;
  departmentName: string;
  employees: LiveAttendanceEmployee[];
}

interface NavItem {
  id: SectionId;
  label: string;
  path: string;
  badge?: string;
}

interface EmployeeDocument {
  title: string;
  type: string;
  status: 'active' | 'remote' | 'leave';
  statusLabel: string;
  fileName?: string;
  sizeLabel?: string;
  previewUrl?: string;
  mimeType?: string;
  storagePath?: string;
}

interface StoredEmployeeFile {
  fileName: string;
  category: string;
  storagePath: string;
  contentType?: string;
  size: number;
}

interface DepartmentGroup {
  name: string;
  employees: Employee[];
  positions: string[];
  rootName?: string;
}

interface OrgChartBranch {
  department: string;
  leader: Employee;
  members: Employee[];
  extraCount: number;
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnInit {
  private readonly apiBaseUrl = 'http://localhost:8080/api/v1';
  private readonly adEmployeesStorageKey = 'hrPeopleOps.adEmployees';
  private readonly adSettingsStorageKey = 'hrPeopleOps.adSettings';
  private readonly employeePhotosStorageKey = 'hrPeopleOps.employeePhotos';
  private readonly employeePhotoAdjustmentsStorageKey = 'hrPeopleOps.employeePhotoAdjustments';
  private readonly adBindDnSessionKey = 'hrPeopleOps.adBindDn';
  private readonly adBindPasswordSessionKey = 'hrPeopleOps.adBindPassword';
  readonly staffOnlySearchBaseDns = [
    'OU=Staff,OU=DMUK,DC=DMUK,DC=EDU',
    'OU=AcademicStaff,OU=DMUK,DC=DMUK,DC=EDU'
  ];

  activeSection: SectionId = 'dashboard';
  selectedEmployeeId: number | null = null;
  selectedDepartmentName: string | null = null;
  selectedOrgDepartmentName = 'all';
  activeEmployeeTab: EmployeeProfileTab = 'job';
  editingPhotoEmployeeId: number | null = null;
  actionMessage = '';
  showAuthPanel = false;
  showDocumentForm = false;
  isAuthenticated = false;
  provisioningPreview: any = null;
  previewDocument: EmployeeDocument | null = null;
  attendanceEmployees: LiveAttendanceEmployee[] = [];
  groupedAttendanceEmployees: AttendanceDepartmentGroup[] = [];
  attendanceHistory: AttendanceHistory[] = [];
  attendanceSearchText = '';
  attendanceSelectedDate = new Date().toISOString().slice(0, 10);
  selectedAttendanceEmployee: LiveAttendanceEmployee | null = null;
  attendanceLastUpdated: Date | null = null;
  private employeePhotos: Record<number, string> = {};
  private employeePhotoAdjustments: Record<number, PhotoAdjustment> = {};
  signedInUser = 'Гость';
  adSettings = {
    url: 'ldap://10.1.10.11:389',
    baseDn: 'DC=DMUK,DC=EDU',
    bindDn: 'zkt@dmuk.edu.kz',
    bindPassword: '',
    userFilter: '(sAMAccountName={username})',
    groupFilter: '(&(objectClass=group)(member={userDn}))',
    searchBaseDns: [...this.staffOnlySearchBaseDns]
  };
  zktecoSettings = {
    host: '10.1.70.2',
    databaseName: '',
    usernamePlaceholder: 'Write in application-secrets.properties',
    passwordPlaceholder: 'Write in application-secrets.properties'
  };
  synologySettings = {
    host: '10.1.30.49',
    rootPath: 'Z:\\Global\\people',
    usernamePlaceholder: 'Write in application-secrets.properties',
    passwordPlaceholder: 'Write in application-secrets.properties'
  };

  constructor(private readonly sanitizer: DomSanitizer) {}

  employees: Employee[] = [];
  private readonly disabledDemoEmployees: Employee[] = [
    { id: 1, name: 'Арман Нурланов', email: 'arman.nurlanov@orca.kz', phone: '+7 701 224 18 90', department: 'Разработка', position: 'Senior Developer', employmentType: 'Полная ставка', status: 'active', statusLabel: 'Активен', hireDate: '02.05.2026', manager: 'Руслан Тажибаев', location: 'Алматы, офис', salaryGrade: 'G7' },
    { id: 2, name: 'Зарина Мухамеджанова', email: 'zarina.m@orca.kz', phone: '+7 705 441 32 18', department: 'HR', position: 'Рекрутер', employmentType: 'Полная ставка', status: 'remote', statusLabel: 'Удаленно', hireDate: '15.01.2025', manager: 'Алия Джаксыбекова', location: 'Астана, удаленно', salaryGrade: 'G5' },
    { id: 3, name: 'Болат Сейткали', email: 'bolat.seitkali@orca.kz', phone: '+7 707 118 44 65', department: 'Финансы', position: 'Финансовый аналитик', employmentType: 'Полная ставка', status: 'active', statusLabel: 'Активен', hireDate: '10.03.2024', manager: 'Мадина Омарова', location: 'Алматы, офис', salaryGrade: 'G6' },
    { id: 4, name: 'Дина Ахметова', email: 'dina.akhmetova@orca.kz', phone: '+7 747 900 21 30', department: 'Маркетинг', position: 'SMM Manager', employmentType: 'Частичная ставка', status: 'leave', statusLabel: 'Отпуск', hireDate: '21.08.2023', manager: 'Айдос Каримов', location: 'Шымкент', salaryGrade: 'G4' }
  ];

  readonly employeeDocuments: Record<number, EmployeeDocument[]> = {};

  readonly groups: { title: string; items: NavItem[] }[] = [
    { title: 'Главная', items: [{ id: 'dashboard', label: 'Дашборд', path: '/' }, { id: 'reports', label: 'Отчеты', path: '/reports' }] },
    { title: 'Кадры', items: [{ id: 'employees', label: 'Сотрудники', path: '/employees' }, { id: 'departments', label: 'Отделы', path: '/departments' }, { id: 'orgchart', label: 'Структура', path: '/org-chart' }, { id: 'positions', label: 'Должности', path: '/positions' }, { id: 'documents', label: 'Документы', path: '/documents' }] },
    { title: 'Учет', items: [{ id: 'attendance', label: 'Посещаемость', path: '/attendance' }, { id: 'vacations', label: 'Отпуска', path: '/vacations' }] },
    { title: 'Система', items: [{ id: 'settings', label: 'Настройки', path: '/settings' }] }
  ];

  get visibleGroups(): { title: string; items: NavItem[] }[] {
    return this.groups
      .map((group) => ({
        ...group,
        items: group.items.filter((item) => item.id !== 'settings' || this.isAuthenticated)
      }))
      .filter((group) => group.items.length > 0);
  }

  readonly sectionTitles: Record<SectionId, string> = {
    dashboard: 'Дашборд',
    employees: 'Сотрудники',
    departments: 'Отделы',
    orgchart: 'Структура',
    positions: 'Должности',
    attendance: 'Посещаемость',
    vacations: 'Отпуска',
    documents: 'Документы',
    reports: 'Отчеты',
    settings: 'Настройки'
  };

  get selectedEmployee(): Employee | undefined {
    return this.employees.find((employee) => employee.id === this.selectedEmployeeId);
  }

  get selectedEmployeeDocuments(): EmployeeDocument[] {
    return this.selectedEmployeeId ? this.employeeDocuments[this.selectedEmployeeId] ?? [] : [];
  }

  get selectedDepartment(): DepartmentGroup | undefined {
    return this.selectedDepartmentName
      ? this.rootDepartmentGroups.find((department) => department.name === this.selectedDepartmentName)
        || this.departmentGroups.find((department) => department.name === this.selectedDepartmentName)
      : undefined;
  }

  get departmentGroups(): DepartmentGroup[] {
    const departments = new Map<string, DepartmentGroup>();

    for (const employee of this.employees) {
      const departmentName = this.cleanDepartmentName(employee.department);
      const key = departmentName.toLocaleLowerCase('ru-RU');
      const group = departments.get(key);

      if (group) {
        group.employees.push({ ...employee, department: group.name });
      } else {
        departments.set(key, {
          name: departmentName,
          employees: [{ ...employee, department: departmentName }],
          positions: []
        });
      }
    }

    return Array.from(departments.values())
      .map((group) => ({
        ...group,
        employees: group.employees.sort((a, b) => a.name.localeCompare(b.name, 'ru-RU')),
        positions: Array.from(new Set(group.employees.map((employee) => employee.position).filter(Boolean))).sort((a, b) => a.localeCompare(b, 'ru-RU'))
      }))
      .sort((a, b) => a.name.localeCompare(b.name, 'ru-RU'));
  }

  get rootDepartmentGroups(): DepartmentGroup[] {
    const departments = new Map<string, DepartmentGroup>();

    for (const employee of this.employees) {
      const rootName = this.cleanDepartmentName(employee.rootDepartment || employee.department);
      const key = rootName.toLocaleLowerCase('ru-RU');
      const group = departments.get(key);

      if (group) {
        group.employees.push(employee);
      } else {
        departments.set(key, {
          name: rootName,
          employees: [employee],
          positions: [],
          rootName
        });
      }
    }

    return Array.from(departments.values())
      .map((group) => ({
        ...group,
        employees: group.employees.sort((a, b) => a.name.localeCompare(b.name, 'ru-RU')),
        positions: Array.from(new Set(group.employees.map((employee) => employee.department).filter(Boolean))).sort((a, b) => a.localeCompare(b, 'ru-RU'))
      }))
      .sort((a, b) => a.name.localeCompare(b.name, 'ru-RU'));
  }

  get positionGroups(): { name: string; count: number }[] {
    const positions = new Map<string, { name: string; count: number }>();

    for (const employee of this.employees) {
      const name = this.cleanPositionName(employee.position);
      const key = name.toLocaleLowerCase('ru-RU');
      const current = positions.get(key);
      if (current) {
        current.count += 1;
      } else {
        positions.set(key, { name, count: 1 });
      }
    }

    return Array.from(positions.values())
      .sort((a, b) => a.name.localeCompare(b.name, 'ru-RU'));
  }

  get orgChartRoot(): Employee | undefined {
    return this.employees.find((employee) => /president|ceo|rector|director/i.test(employee.position))
      || this.employees.find((employee) => /top management|management/i.test(employee.department))
      || this.employees[0];
  }

  get orgChartBranches(): OrgChartBranch[] {
    const rootId = this.orgChartRoot?.id;

    return this.departmentGroups
      .map((department) => {
        const employees = department.employees.filter((employee) => employee.id !== rootId);
        const leader = this.findDepartmentLeader(employees);
        const members = employees.filter((employee) => employee.id !== leader?.id);

        if (!leader) {
          return null;
        }

        return {
          department: department.name,
          leader,
          members: members.slice(0, 4),
          extraCount: Math.max(members.length - 4, 0)
        };
      })
      .filter((branch): branch is OrgChartBranch => Boolean(branch))
      .sort((a, b) => b.members.length - a.members.length || a.department.localeCompare(b.department, 'ru-RU'));
  }

  get visibleOrgChartBranches(): OrgChartBranch[] {
    const branches = this.selectedOrgDepartmentName === 'all'
      ? this.orgChartBranches
      : this.orgChartBranches.filter((branch) => branch.department === this.selectedOrgDepartmentName);

    return branches.slice(0, this.selectedOrgDepartmentName === 'all' ? 6 : 1);
  }

  badgeFor(item: NavItem): string | undefined {
    if (item.id === 'employees') {
      return String(this.employees.length);
    }

    if (item.id === 'departments') {
      return String(this.departmentGroups.length);
    }

    return item.badge;
  }

  documentCount(employeeId: number): number {
    return this.employeeDocuments[employeeId]?.length ?? 0;
  }

  initials(name: string): string {
    return name
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0])
      .join('')
      .toLocaleUpperCase('ru-RU');
  }

  photoFor(employee: Employee): string | undefined {
    return employee.photoUrl || this.employeePhotos[employee.id];
  }

  photoAdjustmentFor(employee: Employee): PhotoAdjustment {
    const adjustment = this.employeePhotoAdjustments[employee.id] ?? { x: 50, y: 50, zoom: 1.2 };
    return { ...adjustment, zoom: Math.max(1.2, adjustment.zoom) };
  }

  photoPositionFor(employee: Employee): string {
    const adjustment = this.photoAdjustmentFor(employee);
    return `${adjustment.x}% ${adjustment.y}%`;
  }

  photoSizeFor(employee: Employee): string {
    return `${Math.round(this.photoAdjustmentFor(employee).zoom * 100)}%`;
  }

  isEditingPhoto(employeeId: number): boolean {
    return this.editingPhotoEmployeeId === employeeId;
  }

  get requiresAuthentication(): boolean {
    return this.activeSection === 'settings';
  }

  get canViewCurrentSection(): boolean {
    return !this.requiresAuthentication || this.isAuthenticated;
  }

  ngOnInit(): void {
    this.syncFromPath();
    const token = localStorage.getItem('accessToken');
    const savedEmployees = localStorage.getItem(this.adEmployeesStorageKey);
    const savedSettings = localStorage.getItem(this.adSettingsStorageKey);
    const savedPhotos = localStorage.getItem(this.employeePhotosStorageKey);
    const savedPhotoAdjustments = localStorage.getItem(this.employeePhotoAdjustmentsStorageKey);

    if (token) {
      this.isAuthenticated = true;
      this.signedInUser = 'Авторизован';
    }
    if (savedEmployees) {
      const parsedEmployees = JSON.parse(savedEmployees) as Employee[];
      if (parsedEmployees.some((employee) => employee.employmentType === 'ZKTeco' || employee.email.endsWith('@zkteco.local'))) {
        localStorage.removeItem(this.adEmployeesStorageKey);
      } else {
        this.employees = parsedEmployees;
      }
    }
    if (savedPhotos) {
      this.employeePhotos = JSON.parse(savedPhotos);
      this.applyEmployeePhotos();
    }
    if (savedPhotoAdjustments) {
      this.employeePhotoAdjustments = JSON.parse(savedPhotoAdjustments);
    }
    if (savedSettings) {
      this.adSettings = { ...this.adSettings, ...JSON.parse(savedSettings) };
    }
    this.adSettings.searchBaseDns = [...this.staffOnlySearchBaseDns];
    this.adSettings.bindDn = sessionStorage.getItem(this.adBindDnSessionKey) || this.adSettings.bindDn;
    this.adSettings.bindPassword = sessionStorage.getItem(this.adBindPasswordSessionKey) || '';
    this.ensureEmployeeDocumentBuckets();

    if (this.selectedEmployeeId) {
      void this.loadEmployeeDocumentsFromStorage(this.selectedEmployeeId);
    }

    if (this.isAuthenticated) {
      void this.loadAdSettingsPreview();
    }

    void this.loadAttendance();
  }

  navigate(event: MouseEvent, section: SectionId): void {
    event.preventDefault();
    this.setSection(section);
  }

  setSection(section: SectionId, updateUrl = true): void {
    this.activeSection = section;
    this.selectedEmployeeId = null;
    this.selectedDepartmentName = null;
    this.showDocumentForm = false;
    this.actionMessage = '';

    if (updateUrl) {
      window.history.pushState({}, '', this.pathFor(section));
    }

    if (section === 'attendance') {
      void this.loadAttendance();
    }
  }

  openEmployee(employeeId: number): void {
    this.activeSection = 'employees';
    this.selectedEmployeeId = employeeId;
    this.activeEmployeeTab = 'job';
    this.editingPhotoEmployeeId = null;
    this.showDocumentForm = false;
    this.previewDocument = null;
    this.actionMessage = '';
    void this.loadEmployeeDocumentsFromStorage(employeeId);
    void this.loadAttendance();
    window.history.pushState({}, '', `/employees/${employeeId}`);
  }

  setEmployeeTab(tab: EmployeeProfileTab): void {
    this.activeEmployeeTab = tab;
    this.editingPhotoEmployeeId = null;
    this.showDocumentForm = false;
    if (tab !== 'documents') {
      this.previewDocument = null;
    }
    this.actionMessage = '';
    if (tab === 'documents' && this.selectedEmployeeId) {
      void this.loadEmployeeDocumentsFromStorage(this.selectedEmployeeId);
    }
    if (tab === 'attendance') {
      void this.loadAttendance();
    }
  }

  backToEmployees(): void {
    if (this.selectedDepartmentName) {
      this.selectedEmployeeId = null;
      this.activeSection = 'departments';
      window.history.pushState({}, '', `/departments/${encodeURIComponent(this.selectedDepartmentName)}`);
      return;
    }

    this.setSection('employees');
  }

  openDepartment(departmentName: string): void {
    this.activeSection = 'departments';
    this.selectedEmployeeId = null;
    this.selectedDepartmentName = departmentName;
    this.showDocumentForm = false;
    this.actionMessage = '';
    window.history.pushState({}, '', `/departments/${encodeURIComponent(departmentName)}`);
  }

  backToDepartments(): void {
    this.selectedDepartmentName = null;
    this.selectedEmployeeId = null;
    this.activeSection = 'departments';
    window.history.pushState({}, '', '/departments');
  }

  toggleAuthPanel(): void {
    this.showAuthPanel = !this.showAuthPanel;
    this.actionMessage = '';
  }

  openCreateUser(): void {
    this.activeSection = 'settings';
    this.selectedEmployeeId = null;
    this.selectedDepartmentName = null;
    this.showDocumentForm = false;
    this.previewDocument = null;
    window.history.pushState({}, '', '/settings');

    if (!this.isAuthenticated) {
      this.showAuthPanel = true;
      this.actionMessage = 'Чтобы создать пользователя AD, сначала войдите как администратор.';
      return;
    }

    this.showAuthPanel = false;
    this.actionMessage = 'Заполните блок "Создание пользователя AD": ФИО, отдел и личную почту.';
  }

  async signInWithPassword(email: string, password: string): Promise<void> {
    try {
      const response = await fetch(`${this.apiBaseUrl}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
      });
      const payload = await response.json();

      if (!response.ok || !payload.success) {
        throw new Error(payload.message || 'Вход не выполнен');
      }

      localStorage.setItem('accessToken', payload.data.accessToken);
      localStorage.setItem('refreshToken', payload.data.refreshToken);
      this.isAuthenticated = true;
      this.signedInUser = payload.data.user.fullName || payload.data.user.email;
      this.showAuthPanel = false;
      this.actionMessage = 'Вход выполнен, JWT-токен сохранен.';
    } catch (error) {
      if (this.tryDemoLogin(email, password)) {
        return;
      }

      this.actionMessage = `Вход не выполнен: ${error instanceof Error ? error.message : 'ошибка подключения'}`;
    }
  }

  async signInWithAd(username: string, password: string): Promise<void> {
    try {
      const response = await fetch(`${this.apiBaseUrl}/auth/ad/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      });
      const payload = await response.json();

      if (!response.ok || !payload.success) {
        throw new Error(payload.message || 'AD-вход не выполнен');
      }

      localStorage.setItem('accessToken', payload.data.accessToken);
      localStorage.setItem('refreshToken', payload.data.refreshToken);
      this.isAuthenticated = true;
      this.signedInUser = payload.data.user.fullName || payload.data.user.email || `AD: ${username}`;
      this.showAuthPanel = false;
      this.actionMessage = 'AD-вход выполнен, JWT-токен сохранен.';
    } catch (error) {
      this.actionMessage = `AD-вход не выполнен: ${error instanceof Error ? error.message : 'ошибка подключения'}`;
    }
  }

  signOut(): void {
    this.isAuthenticated = false;
    this.signedInUser = 'Гость';
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    this.actionMessage = 'Вы вышли из системы.';
  }

  private tryDemoLogin(email: string, password: string): boolean {
    const normalizedEmail = email.trim().toLowerCase();
    const demoUsers = ['admin@demo.com', 'hr@demo.com', 'manager@demo.com', 'employee@demo.com', 'director@demo.com'];

    if (!demoUsers.includes(normalizedEmail) || password !== 'Admin123!') {
      return false;
    }

    localStorage.setItem('accessToken', 'local-demo-token');
    localStorage.setItem('refreshToken', 'local-demo-refresh-token');
    this.isAuthenticated = true;
    this.signedInUser = normalizedEmail === 'admin@demo.com' ? 'Admin Demo' : 'HR Demo';
    this.showAuthPanel = false;
    this.actionMessage = 'Вход выполнен в локальном демо-режиме. Backend сейчас не запущен.';
    return true;
  }

  toggleDocumentForm(): void {
    this.showDocumentForm = !this.showDocumentForm;
    this.actionMessage = '';
  }

  saveDocument(): void {
    if (!this.selectedEmployeeId) {
      return;
    }

    this.employeeDocuments[this.selectedEmployeeId] = [
      ...this.selectedEmployeeDocuments,
      { title: 'Новый документ сотрудника', type: 'PDF', status: 'remote', statusLabel: 'Загружен' }
    ];
    this.showDocumentForm = false;
    this.actionMessage = 'Документ добавлен в карточку сотрудника. Сейчас это локальное демо-сохранение.';
  }

  async uploadEmployeeDocument(event: Event, employeeId: number): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    const employee = this.employees.find((candidate) => candidate.id === employeeId);

    if (!file || !employee) {
      return;
    }

    const extension = file.name.includes('.') ? file.name.split('.').pop()?.toUpperCase() || 'FILE' : 'FILE';
    const title = file.name.replace(/\.[^/.]+$/, '');
    const previewUrl = URL.createObjectURL(file);
    const employeeLogin = this.loginForEmployee(employee);
    const fallbackPath = `Z:\\Global\\people\\${employeeLogin}\\documents\\${file.name}`;
    const document: EmployeeDocument = {
      title,
      type: extension,
      fileName: file.name,
      sizeLabel: this.formatFileSize(file.size),
      previewUrl,
      mimeType: file.type,
      storagePath: fallbackPath,
      status: 'remote',
      statusLabel: 'Сохраняется'
    };

    this.employeeDocuments[employeeId] = [
      ...(this.employeeDocuments[employeeId] ?? []),
      document
    ];

    this.previewDocument = document;
    input.value = '';

    try {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('category', 'documents');
      const response = await fetch(`${this.apiBaseUrl}/employees/${employeeLogin}/files`, {
        method: 'POST',
        headers: this.authorizedHeaders(false),
        body: formData
      });
      const payload = await response.json();

      if (!response.ok || !payload.success) {
        throw new Error(payload.message || 'upload failed');
      }

      document.storagePath = payload.data.storagePath || fallbackPath;
      document.statusLabel = 'Сохранен в папку';
      this.actionMessage = `Документ "${file.name}" сохранен: ${document.storagePath}`;
    } catch (error) {
      document.statusLabel = 'Не сохранен';
      this.actionMessage = `Документ "${file.name}" добавлен для просмотра. В Synology не сохранен, потому что backend/доступ к Z: сейчас недоступен. Плановый путь: ${fallbackPath}`;
    }
  }

  async openDocumentPreview(document: EmployeeDocument): Promise<void> {
    if (!document.previewUrl && this.selectedEmployee) {
      try {
        const employeeLogin = this.loginForEmployee(this.selectedEmployee);
        const fileName = document.fileName || `${document.title}.${document.type.toLowerCase()}`;
        const response = await fetch(`${this.apiBaseUrl}/employees/${employeeLogin}/files/download?category=documents&fileName=${encodeURIComponent(fileName)}`, {
          headers: this.authorizedHeaders(false)
        });

        if (!response.ok) {
          throw new Error('download failed');
        }

        const blob = await response.blob();
        document.previewUrl = URL.createObjectURL(blob);
        document.mimeType = blob.type || document.mimeType;
      } catch (error) {
        this.actionMessage = 'Не удалось открыть документ из папки. Проверьте backend и доступ к Z:.';
      }
    }

    this.previewDocument = document;
  }

  closeDocumentPreview(): void {
    this.previewDocument = null;
  }

  trustedDocumentPreviewUrl(document: EmployeeDocument): SafeResourceUrl | string {
    return document.previewUrl ? this.sanitizer.bypassSecurityTrustResourceUrl(document.previewUrl) : '';
  }

  uploadEmployeePhoto(event: Event, employeeId: number): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    if (!file) {
      return;
    }

    if (!file.type.startsWith('image/')) {
      this.actionMessage = 'Выберите файл изображения для фото сотрудника.';
      input.value = '';
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      const photoUrl = String(reader.result);
      this.employeePhotos[employeeId] = photoUrl;
      this.employeePhotoAdjustments[employeeId] = { x: 50, y: 50, zoom: 1.2 };
      this.editingPhotoEmployeeId = employeeId;
      this.employees = this.employees.map((employee) => employee.id === employeeId ? { ...employee, photoUrl } : employee);
      localStorage.setItem(this.employeePhotosStorageKey, JSON.stringify(this.employeePhotos));
      localStorage.setItem(this.employeePhotoAdjustmentsStorageKey, JSON.stringify(this.employeePhotoAdjustments));
      localStorage.setItem(this.adEmployeesStorageKey, JSON.stringify(this.employees));
      this.actionMessage = 'Фото сотрудника сохранено.';
      input.value = '';
    };
    reader.readAsDataURL(file);
  }

  updateEmployeePhotoAdjustment(employeeId: number, field: PhotoAdjustmentField, value: string): void {
    const numericValue = Number(value);
    const current = this.employeePhotoAdjustments[employeeId] ?? { x: 50, y: 50, zoom: 1 };

    this.employeePhotoAdjustments[employeeId] = {
      ...current,
      [field]: field === 'zoom'
        ? Math.min(2, Math.max(1.2, numericValue))
        : Math.min(100, Math.max(0, numericValue))
    };
    localStorage.setItem(this.employeePhotoAdjustmentsStorageKey, JSON.stringify(this.employeePhotoAdjustments));
  }

  editEmployeePhoto(employeeId: number): void {
    this.editingPhotoEmployeeId = employeeId;
    this.actionMessage = '';
  }

  finishEmployeePhotoEdit(): void {
    this.editingPhotoEmployeeId = null;
    this.actionMessage = 'Положение фото сохранено.';
  }

  resetEmployeePhotoAdjustment(employeeId: number): void {
    this.employeePhotoAdjustments[employeeId] = { x: 50, y: 50, zoom: 1.2 };
    localStorage.setItem(this.employeePhotoAdjustmentsStorageKey, JSON.stringify(this.employeePhotoAdjustments));
  }

  removeEmployeePhoto(employeeId: number): void {
    delete this.employeePhotos[employeeId];
    delete this.employeePhotoAdjustments[employeeId];
    if (this.editingPhotoEmployeeId === employeeId) {
      this.editingPhotoEmployeeId = null;
    }
    this.employees = this.employees.map((employee) => {
      if (employee.id !== employeeId) {
        return employee;
      }

      const { photoUrl, ...employeeWithoutPhoto } = employee;
      return employeeWithoutPhoto;
    });
    localStorage.setItem(this.employeePhotosStorageKey, JSON.stringify(this.employeePhotos));
    localStorage.setItem(this.employeePhotoAdjustmentsStorageKey, JSON.stringify(this.employeePhotoAdjustments));
    localStorage.setItem(this.adEmployeesStorageKey, JSON.stringify(this.employees));
    this.actionMessage = 'Фото сотрудника удалено.';
  }

  async testAdConnection(url: string, baseDn: string, bindDn: string, bindPassword: string, userFilter: string, groupFilter: string, searchBaseDns: string[]): Promise<void> {
    try {
      this.rememberAdCredentials(bindDn, bindPassword);
      const staffSearchBaseDns = this.staffSearchBases(searchBaseDns);
      const response = await fetch(`${this.apiBaseUrl}/admin/integrations/ad/test`, {
        method: 'POST',
        headers: this.authorizedHeaders(),
        body: JSON.stringify({ url, baseDn, bindDn, bindPassword, userFilter, groupFilter, searchBaseDns: staffSearchBaseDns })
      });
      const payload = await response.json();

      if (!response.ok || !payload.success) {
        throw new Error(payload.message || 'AD недоступен');
      }

      this.actionMessage = payload.data.message || 'AD/LDAP подключение успешно проверено.';
    } catch (error) {
      this.actionMessage = `Проверка AD не прошла: ${error instanceof Error ? error.message : 'ошибка подключения'}`;
    }
  }

  async saveAdSettings(url: string, baseDn: string, bindDn: string, bindPassword: string, userFilter: string, groupFilter: string, searchBaseDns: string[]): Promise<void> {
    try {
      this.rememberAdCredentials(bindDn, bindPassword);
      const staffSearchBaseDns = this.staffSearchBases(searchBaseDns);
      const response = await fetch(`${this.apiBaseUrl}/admin/integrations/ad/settings`, {
        method: 'PUT',
        headers: this.authorizedHeaders(),
        body: JSON.stringify({ url, baseDn, bindDn, bindPassword, userFilter, groupFilter, searchBaseDns: staffSearchBaseDns })
      });
      const payload = await response.json();

      if (!response.ok || !payload.success) {
        throw new Error(payload.message || 'Настройки AD не сохранены');
      }

      this.saveAdSettingsLocally(url, baseDn, bindDn, userFilter, groupFilter, staffSearchBaseDns);
      this.actionMessage = 'Настройки AD сохранены в backend runtime-конфиг.';
    } catch (error) {
      this.actionMessage = `Настройки AD не сохранены: ${error instanceof Error ? error.message : 'ошибка подключения'}`;
    }
  }

  async loadAdEmployees(url: string, baseDn: string, bindDn: string, bindPassword: string, userFilter: string, groupFilter: string, searchBaseDns: string[]): Promise<void> {
    try {
      this.rememberAdCredentials(bindDn, bindPassword);
      const staffSearchBaseDns = this.staffSearchBases(searchBaseDns);
      const response = await fetch(`${this.apiBaseUrl}/admin/integrations/ad/users/sync`, {
        method: 'POST',
        headers: this.authorizedHeaders(),
        body: JSON.stringify({ url, baseDn, bindDn, bindPassword, userFilter, groupFilter, searchBaseDns: staffSearchBaseDns })
      });
      const payload = await response.json();

      if (!response.ok || !payload.success) {
        throw new Error(payload.message || 'Сотрудники AD не загружены');
      }

      this.employees = payload.data
        .map((user: any, index: number) => this.toEmployeeFromAd(user, index))
        .sort((a: Employee, b: Employee) => a.department.localeCompare(b.department, 'ru-RU') || a.name.localeCompare(b.name, 'ru-RU'));
      this.applyEmployeePhotos();
      this.ensureEmployeeDocumentBuckets();

      localStorage.setItem(this.adEmployeesStorageKey, JSON.stringify(this.employees));
      this.saveAdSettingsLocally(url, baseDn, bindDn, userFilter, groupFilter, staffSearchBaseDns);
      this.actionMessage = `Загружено сотрудников из AD: ${this.employees.length}`;
      this.setSection('employees');
    } catch (error) {
      this.actionMessage = `Сотрудники AD не загружены: ${error instanceof Error ? error.message : 'ошибка подключения'}`;
    }
  }

  async previewProvisioning(fullName: string, department: string, personalEmail: string): Promise<void> {
    try {
      const response = await fetch(`${this.apiBaseUrl}/admin/provisioning/users/preview`, {
        method: 'POST',
        headers: { ...this.authorizedHeaders(), 'Content-Type': 'application/json' },
        body: JSON.stringify({ fullName, department, personalEmail })
      });
      const payload = await response.json();

      if (!response.ok || !payload.success) {
        throw new Error(payload.message || 'preview failed');
      }

      this.provisioningPreview = payload.data;
      this.actionMessage = 'Provisioning preview generated.';
    } catch (error) {
      this.actionMessage = `Provisioning preview failed: ${error instanceof Error ? error.message : 'connection error'}`;
    }
  }

  async createProvisionedUser(fullName: string, department: string, personalEmail: string): Promise<void> {
    try {
      const response = await fetch(`${this.apiBaseUrl}/admin/provisioning/users/preview`, {
        method: 'POST',
        headers: { ...this.authorizedHeaders(), 'Content-Type': 'application/json' },
        body: JSON.stringify({ fullName, department, personalEmail })
      });
      const payload = await response.json();

      if (!response.ok || !payload.success) {
        throw new Error(payload.message || 'create failed');
      }

      this.provisioningPreview = payload.data;
      const nextId = Math.max(0, ...this.employees.map((employee) => employee.id)) + 1;
      const existingIndex = this.employees.findIndex((employee) => employee.email.toLowerCase() === payload.data.corporateEmail.toLowerCase());
      const employee: Employee = {
        id: existingIndex >= 0 ? this.employees[existingIndex].id : nextId,
        name: this.cleanPersonName(payload.data.fullName),
        email: payload.data.corporateEmail,
        phone: '',
        department: this.cleanDepartmentName(payload.data.department),
        position: 'Сотрудник',
        employmentType: 'AD account',
        status: 'active',
        statusLabel: 'Активен',
        hireDate: '-',
        manager: '',
        location: 'DMUK',
        salaryGrade: ''
      };

      if (existingIndex >= 0) {
        this.employees = this.employees.map((item, index) => index === existingIndex ? employee : item);
      } else {
        this.employees = [...this.employees, employee];
      }

      this.employees = this.employees
        .sort((a, b) => a.department.localeCompare(b.department, 'ru-RU') || a.name.localeCompare(b.name, 'ru-RU'));
      this.ensureEmployeeDocumentBuckets();
      localStorage.setItem(this.adEmployeesStorageKey, JSON.stringify(this.employees));
      this.setSection('employees');
      this.actionMessage = `Пользователь ${employee.name} добавлен на сайт: ${employee.email}. AD и отправка письма пока не выполнены: backend сейчас работает в dry-run режиме.`;
    } catch (error) {
      this.actionMessage = `Пользователь не создан: ${error instanceof Error ? error.message : 'ошибка подключения'}`;
    }
  }

  handlePrimaryAction(): void {
    this.actionMessage = this.selectedEmployee
      ? `Открыта форма редактирования сотрудника: ${this.selectedEmployee.name}.`
      : `Действие для раздела "${this.sectionTitles[this.activeSection]}" готово к подключению к базе данных.`;
  }

  openReport(name: string): void {
    this.actionMessage = `Отчет "${name}" открыт в демо-режиме.`;
  }

  async loadAttendance(): Promise<void> {
    try {
      const response = await fetch(`${this.apiBaseUrl}/attendance/inside`, {
        headers: this.authorizedHeaders(false)
      });
      const payload = await response.json();

      if (!response.ok || !payload.success) {
        throw new Error(payload.message || 'attendance unavailable');
      }

      this.attendanceEmployees = payload.data ?? [];
      this.applyAttendanceFilters();
      this.attendanceLastUpdated = new Date();
    } catch (error) {
      this.actionMessage = `ZKTeco attendance is not loaded: ${error instanceof Error ? error.message : 'connection error'}`;
      this.attendanceEmployees = [];
      this.groupedAttendanceEmployees = [];
    }
  }

  applyAttendanceFilters(): void {
    const text = this.attendanceSearchText.trim().toLowerCase();
    const filtered = this.attendanceEmployees.filter((employee) => {
      const fullName = `${employee.name || ''} ${employee.lastName || ''}`.toLowerCase();
      return this.isKnownAdAttendance(employee)
        && (!text
          || fullName.includes(text)
          || (employee.pin || '').toLowerCase().includes(text)
          || (employee.departmentName || '').toLowerCase().includes(text));
    });

    const map = new Map<string, AttendanceDepartmentGroup>();
    for (const employee of filtered) {
      const key = `${employee.rootDepartment || 'Other'}__${employee.departmentName || 'No department'}`;
      if (!map.has(key)) {
        map.set(key, {
          rootDepartment: employee.rootDepartment || 'Other',
          departmentName: employee.departmentName || 'No department',
          employees: []
        });
      }
      map.get(key)!.employees.push(employee);
    }

    this.groupedAttendanceEmployees = Array.from(map.values())
      .sort((a, b) => a.departmentName.localeCompare(b.departmentName, 'ru-RU'));
  }

  openAttendanceEmployee(employee: LiveAttendanceEmployee): void {
    this.selectedAttendanceEmployee = employee;
    void this.loadAttendanceHistory(employee.pin);
  }

  async loadAttendanceHistory(pin?: string): Promise<void> {
    const targetPin = pin || this.selectedAttendanceEmployee?.pin || this.selectedEmployeeAttendance?.pin;
    if (!targetPin) {
      this.attendanceHistory = [];
      return;
    }

    try {
      const response = await fetch(`${this.apiBaseUrl}/attendance/history?pin=${encodeURIComponent(targetPin)}&date=${this.attendanceSelectedDate}`, {
        headers: this.authorizedHeaders(false)
      });
      const payload = await response.json();

      if (!response.ok || !payload.success) {
        throw new Error(payload.message || 'history unavailable');
      }

      this.attendanceHistory = payload.data ?? [];
    } catch (error) {
      this.attendanceHistory = [];
      this.actionMessage = `Attendance history is not loaded: ${error instanceof Error ? error.message : 'connection error'}`;
    }
  }

  closeAttendanceEmployee(): void {
    this.selectedAttendanceEmployee = null;
    this.attendanceHistory = [];
  }

  attendancePhotoUrl(employee: LiveAttendanceEmployee): string {
    return employee.photoPath
      ? `${this.apiBaseUrl}/attendance/photo?path=${encodeURIComponent(employee.photoPath)}`
      : '';
  }

  attendanceInitials(employee: LiveAttendanceEmployee): string {
    return `${employee.name?.charAt(0) || ''}${employee.lastName?.charAt(0) || ''}`.toUpperCase() || '?';
  }

  get insideCount(): number {
    return this.attendanceEmployees.filter((employee) => employee.status === 'INSIDE' && this.isKnownAdAttendance(employee)).length;
  }

  get outsideCount(): number {
    return this.attendanceEmployees.filter((employee) => employee.status === 'OUTSIDE' && this.isKnownAdAttendance(employee)).length;
  }

  get selectedEmployeeAttendance(): LiveAttendanceEmployee | undefined {
    const employee = this.selectedEmployee;
    if (!employee) {
      return undefined;
    }

    const login = this.loginForEmployee(employee).toLowerCase();
    const pin = (employee.zktecoPin || employee.adUsername || login).toLowerCase();
    return this.attendanceEmployees.find((candidate) =>
      (candidate.pin || '').toLowerCase() === pin
      || `${candidate.name || ''} ${candidate.lastName || ''}`.trim().toLowerCase() === employee.name.toLowerCase()
    );
  }

  setOrgDepartment(departmentName: string): void {
    this.selectedOrgDepartmentName = departmentName;
  }

  legacyProfileVisible(): boolean {
    return false;
  }

  @HostListener('window:popstate')
  private syncFromPath(): void {
    const cleanPath = window.location.pathname.replace(/\/$/, '') || '/';
    const employeeMatch = cleanPath.match(/^\/employees\/(\d+)$/);
    const departmentMatch = cleanPath.match(/^\/departments\/(.+)$/);

    if (employeeMatch) {
      this.activeSection = 'employees';
      this.selectedEmployeeId = Number(employeeMatch[1]);
      return;
    }

    if (departmentMatch) {
      this.activeSection = 'departments';
      this.selectedDepartmentName = decodeURIComponent(departmentMatch[1]);
      this.selectedEmployeeId = null;
      return;
    }

    this.selectedEmployeeId = null;
    this.selectedDepartmentName = null;
    this.activeSection = this.sectionFor(cleanPath);
  }

  private sectionFor(path: string): SectionId {
    const match = this.groups.flatMap((group) => group.items).find((item) => item.path === path);
    return match?.id ?? 'dashboard';
  }

  private pathFor(section: SectionId): string {
    return this.groups.flatMap((group) => group.items).find((item) => item.id === section)?.path ?? '/';
  }

  private authorizedHeaders(includeJsonContentType = true): HeadersInit {
    const token = localStorage.getItem('accessToken');
    return {
      ...(includeJsonContentType ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    };
  }

  private async loadAdSettingsPreview(): Promise<void> {
    try {
      const response = await fetch(`${this.apiBaseUrl}/admin/integrations/ad/settings/preview`, {
        headers: this.authorizedHeaders(false)
      });
      const payload = await response.json();

      if (!response.ok || !payload.success) {
        return;
      }

      this.adSettings = {
        ...this.adSettings,
        url: payload.data.url || this.adSettings.url,
        baseDn: payload.data.baseDn || this.adSettings.baseDn,
        bindDn: sessionStorage.getItem(this.adBindDnSessionKey) || payload.data.bindDn || this.adSettings.bindDn,
        userFilter: payload.data.userFilter || this.adSettings.userFilter,
        groupFilter: payload.data.groupFilter || this.adSettings.groupFilter,
        searchBaseDns: this.staffSearchBases(payload.data.searchBaseDns)
      };
      this.adSettings.bindPassword = sessionStorage.getItem(this.adBindPasswordSessionKey) || this.adSettings.bindPassword;
    } catch (error) {
      // Cached settings remain available if backend is not reachable.
    }
  }

  private toEmployeeFromAd(user: any, index: number): Employee {
    const username = String(user.username || '').trim().toLowerCase();
    const email = String(user.email || (username ? `${username}@dmuk.edu.kz` : `ad.user${index + 1}@dmuk.edu.kz`)).trim().toLowerCase();

    return {
      id: index + 1,
      name: this.cleanPersonName(user.displayName || user.username || email.split('@')[0] || `AD user ${index + 1}`),
      email,
      adUsername: username,
      zktecoPin: username,
      rootDepartment: this.cleanDepartmentName(user.department || this.departmentFromDn(user.dn) || 'Без отдела'),
      phone: '',
      department: this.cleanDepartmentName(user.department || this.departmentFromDn(user.dn) || 'Без отдела'),
      position: this.cleanPositionName(user.title || 'Сотрудник'),
      employmentType: 'AD account',
      status: 'active',
      statusLabel: 'Активен',
      hireDate: '-',
      manager: this.managerName(user.manager),
      location: 'DMUK',
      salaryGrade: ''
    };
  }

  private ensureEmployeeDocumentBuckets(): void {
    for (const employee of this.employees) {
      this.employeeDocuments[employee.id] = this.employeeDocuments[employee.id] ?? [];
    }
  }

  private isKnownAdAttendance(attendance: LiveAttendanceEmployee): boolean {
    if (!this.employees.length) {
      return false;
    }

    const pin = (attendance.pin || '').toLowerCase();
    const fullName = `${attendance.name || ''} ${attendance.lastName || ''}`.trim().toLowerCase();

    return this.employees.some((employee) => {
      const login = this.loginForEmployee(employee).toLowerCase();
      return (employee.zktecoPin || '').toLowerCase() === pin
        || (employee.adUsername || '').toLowerCase() === pin
        || login === pin
        || employee.name.toLowerCase() === fullName;
    });
  }

  private async loadEmployeeDocumentsFromStorage(employeeId: number): Promise<void> {
    const employee = this.employees.find((candidate) => candidate.id === employeeId);

    if (!employee) {
      return;
    }

    try {
      const employeeLogin = this.loginForEmployee(employee);
      const response = await fetch(`${this.apiBaseUrl}/employees/${employeeLogin}/files?category=documents`, {
        headers: this.authorizedHeaders(false)
      });
      const payload = await response.json();

      if (!response.ok || !payload.success) {
        throw new Error(payload.message || 'list failed');
      }

      const storedDocuments = (payload.data as StoredEmployeeFile[]).map((file) => this.toEmployeeDocument(file));
      const currentDocuments = this.employeeDocuments[employeeId] ?? [];
      const storedFileNames = new Set(storedDocuments.map((document) => document.fileName));
      const localOnlyDocuments = currentDocuments.filter((document) => document.storagePath && !storedFileNames.has(document.fileName));

      this.employeeDocuments[employeeId] = [
        ...storedDocuments,
        ...localOnlyDocuments
      ];
    } catch (error) {
      // If backend or Z: is temporarily unavailable, keep the current visible list.
    }
  }

  private toEmployeeDocument(file: StoredEmployeeFile): EmployeeDocument {
    const extension = (file.fileName.split('.').pop() || 'file').toUpperCase();

    return {
      title: file.fileName.replace(/\.[^/.]+$/, ''),
      type: extension,
      fileName: file.fileName,
      sizeLabel: this.formatFileSize(file.size),
      mimeType: file.contentType,
      storagePath: file.storagePath,
      status: 'remote',
      statusLabel: 'Сохранен в папку'
    };
  }

  private saveAdSettingsLocally(url: string, baseDn: string, bindDn: string, userFilter: string, groupFilter: string, searchBaseDns: string[]): void {
    this.adSettings = { ...this.adSettings, url, baseDn, bindDn, userFilter, groupFilter, searchBaseDns: this.staffSearchBases(searchBaseDns) };
    const { bindPassword, ...settingsToPersist } = this.adSettings;
    localStorage.setItem(this.adSettingsStorageKey, JSON.stringify(settingsToPersist));
  }

  private staffSearchBases(values?: string[]): string[] {
    const normalizedInput = new Set((values ?? [])
      .map((value) => this.normalizeDn(value))
      .filter(Boolean));
    const selected = this.staffOnlySearchBaseDns
      .filter((value) => normalizedInput.size === 0 || normalizedInput.has(this.normalizeDn(value)));

    return selected.length ? selected : [...this.staffOnlySearchBaseDns];
  }

  private cleanDepartmentName(department: string | null | undefined): string {
    const normalized = (department || 'Без отдела').trim().replace(/\s+/g, ' ');

    if (!normalized) {
      return 'Без отдела';
    }

    if (normalized === normalized.toUpperCase() && normalized.length <= 3) {
      return normalized;
    }

    if (normalized === normalized.toUpperCase()) {
      return normalized
        .toLocaleLowerCase('ru-RU')
        .replace(/(^|\s|-)(\S)/g, (match) => match.toLocaleUpperCase('ru-RU'));
    }

    return normalized;
  }

  private cleanPersonName(value: string): string {
    return value
      .replace(/\s+/g, ' ')
      .trim()
      .replace(/^cn=/i, '');
  }

  private cleanPositionName(value: string): string {
    const normalized = value.replace(/\s+/g, ' ').trim();
    return normalized || 'Сотрудник';
  }

  private managerName(value: string | null | undefined): string {
    if (!value) {
      return '';
    }

    const cn = value.match(/CN=([^,]+)/i)?.[1];
    return this.cleanPersonName(cn || value);
  }

  private departmentFromDn(value: string | null | undefined): string {
    if (!value) {
      return '';
    }

    const ouValues = Array.from(value.matchAll(/OU=([^,]+)/gi)).map((match) => match[1]);
    const ignored = new Set(['staff', 'academicstaff', 'students', 'test', 'f2025', 'lab accounts', 'labaccounts', 'dmuk']);
    return ouValues.find((ou) => !ignored.has(ou.toLowerCase())) || '';
  }

  private normalizeDn(value: string | null | undefined): string {
    return (value || '').replace(/\s+/g, '').toLowerCase();
  }

  private findDepartmentLeader(employees: Employee[]): Employee | undefined {
    return employees.find((employee) => /president|director|head|chief|manager|lead|cto|cfo|cro|hr/i.test(employee.position))
      || employees[0];
  }

  private applyEmployeePhotos(): void {
    this.employees = this.employees.map((employee) => ({
      ...employee,
      ...(this.employeePhotos[employee.id] ? { photoUrl: this.employeePhotos[employee.id] } : {})
    }));
  }

  private formatFileSize(size: number): string {
    if (size < 1024) {
      return `${size} B`;
    }

    if (size < 1024 * 1024) {
      return `${Math.round(size / 1024)} KB`;
    }

    return `${(size / 1024 / 1024).toFixed(1)} MB`;
  }

  private loginForEmployee(employee: Employee): string {
    if (employee.email?.includes('@')) {
      return employee.email.split('@')[0].toLowerCase();
    }

    const parts = employee.name.trim().toLowerCase().split(/\s+/);
    if (parts.length >= 2) {
      return `${parts[1].slice(0, 1)}.${parts[0]}`.replace(/[^a-z0-9.]/g, '');
    }

    return `employee-${employee.id}`;
  }

  rememberAdCredentials(bindDn: string, bindPassword: string): void {
    this.adSettings.bindDn = bindDn;
    this.adSettings.bindPassword = bindPassword;
    sessionStorage.setItem(this.adBindDnSessionKey, bindDn);
    sessionStorage.setItem(this.adBindPasswordSessionKey, bindPassword);
  }
}
