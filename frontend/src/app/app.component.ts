import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

type SectionId = 'dashboard' | 'employees' | 'departments' | 'orgchart' | 'positions' | 'attendance' | 'vacations' | 'documents' | 'reports' | 'settings' | 'registration';
type EmployeeProfileTab = 'personal' | 'job' | 'attendance' | 'timeoff' | 'performance' | 'documents' | 'onboarding' | 'tasks' | 'more';
type PhotoAdjustmentField = 'x' | 'y' | 'zoom';
type SettingsTab = 'ad' | 'registration' | 'requests' | 'users' | 'integrations';

interface PhoneCountry {
  name: string;
  code: string;
  dialCode: string;
  flag: string;
  minLength: number;
  maxLength: number;
}

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

interface ZktecoPersonPhoto {
  pin: string;
  name: string;
  lastName: string;
  photoPath: string;
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

interface RegistrationRequestItem {
  id: string;
  fullName: string;
  firstName?: string;
  lastName?: string;
  department?: string;
  phone: string;
  personalEmail: string;
  identityDocumentFileId: string;
  facePhotoFileId: string;
  status: string;
  corporateEmail?: string;
  rejectionReason?: string;
  createdAt?: string;
}

interface AdminUserItem {
  id: string;
  email: string;
  status: 'ACTIVE' | 'BLOCKED' | 'DISABLED';
  lastLoginAt?: string;
  failedLoginAttempts?: number;
  lockedAt?: string;
  employeeId?: string;
  roles: string[];
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnDestroy, OnInit {
  private readonly apiBaseUrl = window.location.port === '4200'
    ? 'http://127.0.0.1:8080/api/v1'
    : '/api/v1';
  private readonly adEmployeesStorageKey = 'hrPeopleOps.adEmployees';
  private readonly adSettingsStorageKey = 'hrPeopleOps.adSettings';
  private readonly employeePhotosStorageKey = 'hrPeopleOps.employeePhotos';
  private readonly zktecoPhotoCacheStorageKey = 'hrPeopleOps.zktecoPhotoCache';
  private readonly employeePhotoAdjustmentsStorageKey = 'hrPeopleOps.employeePhotoAdjustments';
  private readonly adBindDnSessionKey = 'hrPeopleOps.adBindDn';
  private readonly adBindPasswordSessionKey = 'hrPeopleOps.adBindPassword';
  readonly staffOnlySearchBaseDns = [
    'OU=Staff,OU=DMUK,DC=DMUK,DC=EDU',
    'OU=AcademicStaff,OU=DMUK,DC=DMUK,DC=EDU'
  ];

  activeSection: SectionId = 'dashboard';
  activeSettingsTab: SettingsTab = 'registration';
  selectedEmployeeId: number | null = null;
  selectedDepartmentName: string | null = null;
  selectedOrgDepartmentName = 'all';
  activeEmployeeTab: EmployeeProfileTab = 'job';
  editingPhotoEmployeeId: number | null = null;
  actionMessage = '';
  showAuthPanel = false;
  showDocumentForm = false;
  isAuthenticated = false;
  currentUserRoles: string[] = [];
  registrationToken = '';
  registrationLink = '';
  selectedPhoneCountryCode = 'KZ';
  faceCaptureStatus: 'idle' | 'checking' | 'valid' | 'invalid' | 'captured' = 'idle';
  faceCaptureMessage = 'Откройте камеру и поместите лицо внутри овала.';
  faceCapturePreviewUrl = '';
  faceCameraActive = false;
  faceCameraSupported = typeof navigator !== 'undefined' && Boolean(navigator.mediaDevices?.getUserMedia);
  private capturedFaceFile: File | null = null;
  private faceCameraStream: MediaStream | null = null;
  private faceValidationTimer: number | null = null;
  registrationRequests: RegistrationRequestItem[] = [];
  selectedRegistrationRequest: RegistrationRequestItem | null = null;
  registrationSubmitting = false;
  registrationSubmitted = false;
  adminUsers: AdminUserItem[] = [];
  employeeEditDraft: Partial<Employee> | null = null;
  draggedEmployeeId: number | null = null;
  provisioningPreview: any = null;
  previewDocument: EmployeeDocument | null = null;
  attendanceEmployees: LiveAttendanceEmployee[] = [];
  zktecoPersonPhotos: ZktecoPersonPhoto[] = [];
  groupedAttendanceEmployees: AttendanceDepartmentGroup[] = [];
  attendanceHistory: AttendanceHistory[] = [];
  attendanceSearchText = '';
  attendanceSelectedDate = new Date().toISOString().slice(0, 10);
  selectedAttendanceEmployee: LiveAttendanceEmployee | null = null;
  attendanceLastUpdated: Date | null = null;
  attendancePhotoDataUrls: Record<string, string> = {};
  private employeePhotos: Record<number, string> = {};
  private zktecoPhotoCache: Record<string, string> = {};
  private brokenPhotoUrls = new Set<string>();
  private attendancePhotoLoadFailures = new Set<string>();
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

  readonly phoneCountries: PhoneCountry[] = [
    { name: 'Kazakhstan', code: 'KZ', dialCode: '+7', flag: '🇰🇿', minLength: 10, maxLength: 10 },
    { name: 'Russia', code: 'RU', dialCode: '+7', flag: '🇷🇺', minLength: 10, maxLength: 10 },
    { name: 'Kyrgyzstan', code: 'KG', dialCode: '+996', flag: '🇰🇬', minLength: 9, maxLength: 9 },
    { name: 'Uzbekistan', code: 'UZ', dialCode: '+998', flag: '🇺🇿', minLength: 9, maxLength: 9 },
    { name: 'United States', code: 'US', dialCode: '+1', flag: '🇺🇸', minLength: 10, maxLength: 10 },
    { name: 'United Kingdom', code: 'GB', dialCode: '+44', flag: '🇬🇧', minLength: 10, maxLength: 10 },
    { name: 'Turkey', code: 'TR', dialCode: '+90', flag: '🇹🇷', minLength: 10, maxLength: 10 },
    { name: 'China', code: 'CN', dialCode: '+86', flag: '🇨🇳', minLength: 11, maxLength: 11 },
    { name: 'India', code: 'IN', dialCode: '+91', flag: '🇮🇳', minLength: 10, maxLength: 10 },
    { name: 'Germany', code: 'DE', dialCode: '+49', flag: '🇩🇪', minLength: 7, maxLength: 12 },
    { name: 'France', code: 'FR', dialCode: '+33', flag: '🇫🇷', minLength: 9, maxLength: 9 },
    { name: 'Italy', code: 'IT', dialCode: '+39', flag: '🇮🇹', minLength: 9, maxLength: 10 },
    { name: 'Spain', code: 'ES', dialCode: '+34', flag: '🇪🇸', minLength: 9, maxLength: 9 },
    { name: 'United Arab Emirates', code: 'AE', dialCode: '+971', flag: '🇦🇪', minLength: 9, maxLength: 9 },
    { name: 'Saudi Arabia', code: 'SA', dialCode: '+966', flag: '🇸🇦', minLength: 9, maxLength: 9 },
    { name: 'South Korea', code: 'KR', dialCode: '+82', flag: '🇰🇷', minLength: 9, maxLength: 10 },
    { name: 'Japan', code: 'JP', dialCode: '+81', flag: '🇯🇵', minLength: 10, maxLength: 10 },
    { name: 'Canada', code: 'CA', dialCode: '+1', flag: '🇨🇦', minLength: 10, maxLength: 10 },
    { name: 'Australia', code: 'AU', dialCode: '+61', flag: '🇦🇺', minLength: 9, maxLength: 9 },
    { name: 'Ukraine', code: 'UA', dialCode: '+380', flag: '🇺🇦', minLength: 9, maxLength: 9 },
    { name: 'Afghanistan', code: 'AF', dialCode: '+93', flag: '🇦🇫', minLength: 9, maxLength: 9 },
    { name: 'Albania', code: 'AL', dialCode: '+355', flag: '🇦🇱', minLength: 8, maxLength: 9 },
    { name: 'Algeria', code: 'DZ', dialCode: '+213', flag: '🇩🇿', minLength: 9, maxLength: 9 },
    { name: 'Argentina', code: 'AR', dialCode: '+54', flag: '🇦🇷', minLength: 10, maxLength: 10 },
    { name: 'Armenia', code: 'AM', dialCode: '+374', flag: '🇦🇲', minLength: 8, maxLength: 8 },
    { name: 'Austria', code: 'AT', dialCode: '+43', flag: '🇦🇹', minLength: 7, maxLength: 13 },
    { name: 'Azerbaijan', code: 'AZ', dialCode: '+994', flag: '🇦🇿', minLength: 9, maxLength: 9 },
    { name: 'Bahrain', code: 'BH', dialCode: '+973', flag: '🇧🇭', minLength: 8, maxLength: 8 },
    { name: 'Bangladesh', code: 'BD', dialCode: '+880', flag: '🇧🇩', minLength: 10, maxLength: 10 },
    { name: 'Belarus', code: 'BY', dialCode: '+375', flag: '🇧🇾', minLength: 9, maxLength: 9 },
    { name: 'Belgium', code: 'BE', dialCode: '+32', flag: '🇧🇪', minLength: 8, maxLength: 9 },
    { name: 'Brazil', code: 'BR', dialCode: '+55', flag: '🇧🇷', minLength: 10, maxLength: 11 },
    { name: 'Bulgaria', code: 'BG', dialCode: '+359', flag: '🇧🇬', minLength: 8, maxLength: 9 },
    { name: 'Chile', code: 'CL', dialCode: '+56', flag: '🇨🇱', minLength: 9, maxLength: 9 },
    { name: 'Colombia', code: 'CO', dialCode: '+57', flag: '🇨🇴', minLength: 10, maxLength: 10 },
    { name: 'Croatia', code: 'HR', dialCode: '+385', flag: '🇭🇷', minLength: 8, maxLength: 9 },
    { name: 'Czechia', code: 'CZ', dialCode: '+420', flag: '🇨🇿', minLength: 9, maxLength: 9 },
    { name: 'Denmark', code: 'DK', dialCode: '+45', flag: '🇩🇰', minLength: 8, maxLength: 8 },
    { name: 'Egypt', code: 'EG', dialCode: '+20', flag: '🇪🇬', minLength: 10, maxLength: 10 },
    { name: 'Estonia', code: 'EE', dialCode: '+372', flag: '🇪🇪', minLength: 7, maxLength: 8 },
    { name: 'Finland', code: 'FI', dialCode: '+358', flag: '🇫🇮', minLength: 7, maxLength: 12 },
    { name: 'Georgia', code: 'GE', dialCode: '+995', flag: '🇬🇪', minLength: 9, maxLength: 9 },
    { name: 'Greece', code: 'GR', dialCode: '+30', flag: '🇬🇷', minLength: 10, maxLength: 10 },
    { name: 'Hong Kong', code: 'HK', dialCode: '+852', flag: '🇭🇰', minLength: 8, maxLength: 8 },
    { name: 'Hungary', code: 'HU', dialCode: '+36', flag: '🇭🇺', minLength: 8, maxLength: 9 },
    { name: 'Indonesia', code: 'ID', dialCode: '+62', flag: '🇮🇩', minLength: 9, maxLength: 12 },
    { name: 'Iran', code: 'IR', dialCode: '+98', flag: '🇮🇷', minLength: 10, maxLength: 10 },
    { name: 'Iraq', code: 'IQ', dialCode: '+964', flag: '🇮🇶', minLength: 10, maxLength: 10 },
    { name: 'Ireland', code: 'IE', dialCode: '+353', flag: '🇮🇪', minLength: 9, maxLength: 9 },
    { name: 'Israel', code: 'IL', dialCode: '+972', flag: '🇮🇱', minLength: 9, maxLength: 9 },
    { name: 'Jordan', code: 'JO', dialCode: '+962', flag: '🇯🇴', minLength: 9, maxLength: 9 },
    { name: 'Kuwait', code: 'KW', dialCode: '+965', flag: '🇰🇼', minLength: 8, maxLength: 8 },
    { name: 'Latvia', code: 'LV', dialCode: '+371', flag: '🇱🇻', minLength: 8, maxLength: 8 },
    { name: 'Lebanon', code: 'LB', dialCode: '+961', flag: '🇱🇧', minLength: 7, maxLength: 8 },
    { name: 'Lithuania', code: 'LT', dialCode: '+370', flag: '🇱🇹', minLength: 8, maxLength: 8 },
    { name: 'Malaysia', code: 'MY', dialCode: '+60', flag: '🇲🇾', minLength: 9, maxLength: 10 },
    { name: 'Mexico', code: 'MX', dialCode: '+52', flag: '🇲🇽', minLength: 10, maxLength: 10 },
    { name: 'Moldova', code: 'MD', dialCode: '+373', flag: '🇲🇩', minLength: 8, maxLength: 8 },
    { name: 'Mongolia', code: 'MN', dialCode: '+976', flag: '🇲🇳', minLength: 8, maxLength: 8 },
    { name: 'Morocco', code: 'MA', dialCode: '+212', flag: '🇲🇦', minLength: 9, maxLength: 9 },
    { name: 'Netherlands', code: 'NL', dialCode: '+31', flag: '🇳🇱', minLength: 9, maxLength: 9 },
    { name: 'New Zealand', code: 'NZ', dialCode: '+64', flag: '🇳🇿', minLength: 8, maxLength: 10 },
    { name: 'Norway', code: 'NO', dialCode: '+47', flag: '🇳🇴', minLength: 8, maxLength: 8 },
    { name: 'Oman', code: 'OM', dialCode: '+968', flag: '🇴🇲', minLength: 8, maxLength: 8 },
    { name: 'Pakistan', code: 'PK', dialCode: '+92', flag: '🇵🇰', minLength: 10, maxLength: 10 },
    { name: 'Philippines', code: 'PH', dialCode: '+63', flag: '🇵🇭', minLength: 10, maxLength: 10 },
    { name: 'Poland', code: 'PL', dialCode: '+48', flag: '🇵🇱', minLength: 9, maxLength: 9 },
    { name: 'Portugal', code: 'PT', dialCode: '+351', flag: '🇵🇹', minLength: 9, maxLength: 9 },
    { name: 'Qatar', code: 'QA', dialCode: '+974', flag: '🇶🇦', minLength: 8, maxLength: 8 },
    { name: 'Romania', code: 'RO', dialCode: '+40', flag: '🇷🇴', minLength: 9, maxLength: 9 },
    { name: 'Serbia', code: 'RS', dialCode: '+381', flag: '🇷🇸', minLength: 8, maxLength: 9 },
    { name: 'Singapore', code: 'SG', dialCode: '+65', flag: '🇸🇬', minLength: 8, maxLength: 8 },
    { name: 'Slovakia', code: 'SK', dialCode: '+421', flag: '🇸🇰', minLength: 9, maxLength: 9 },
    { name: 'South Africa', code: 'ZA', dialCode: '+27', flag: '🇿🇦', minLength: 9, maxLength: 9 },
    { name: 'Sweden', code: 'SE', dialCode: '+46', flag: '🇸🇪', minLength: 7, maxLength: 10 },
    { name: 'Switzerland', code: 'CH', dialCode: '+41', flag: '🇨🇭', minLength: 9, maxLength: 9 },
    { name: 'Tajikistan', code: 'TJ', dialCode: '+992', flag: '🇹🇯', minLength: 9, maxLength: 9 },
    { name: 'Thailand', code: 'TH', dialCode: '+66', flag: '🇹🇭', minLength: 9, maxLength: 9 },
    { name: 'Turkmenistan', code: 'TM', dialCode: '+993', flag: '🇹🇲', minLength: 8, maxLength: 8 },
    { name: 'Vietnam', code: 'VN', dialCode: '+84', flag: '🇻🇳', minLength: 9, maxLength: 10 }
  ];

  employees: Employee[] = [];
  private employeesAutoLoadInProgress = false;
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
        items: group.items.filter((item) => item.id !== 'settings' || this.isAdminUser)
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
    ,registration: 'Регистрация'
  };

  get isAdminUser(): boolean {
    return this.currentUserRoles.some((role) => ['SUPER_ADMIN', 'HR_ADMIN', 'INTEGRATION_ADMIN'].includes(role));
  }

  get isRegistrationPage(): boolean {
    return this.activeSection === 'registration';
  }

  get isMobileRegistrationDevice(): boolean {
    if (typeof navigator === 'undefined') {
      return false;
    }
    return window.matchMedia('(max-width: 768px), (pointer: coarse)').matches
      || /Android|iPhone|iPad|iPod|Mobile/i.test(navigator.userAgent);
  }

  get faceCaptureClass(): string {
    return `face-capture ${this.faceCaptureStatus}`;
  }

  get selectedPhoneCountry(): PhoneCountry {
    return this.phoneCountries.find((country) => country.code === this.selectedPhoneCountryCode) || this.phoneCountries[0];
  }

  get registrationDepartmentOptions(): string[] {
    const names = this.departmentGroups
      .map((department) => this.cleanDepartmentName(department.name))
      .filter((department) => department && department !== 'Без отдела');
    return Array.from(new Set([...names, 'Staff', 'AcademicStaff', 'IT', 'HR']));
  }

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
    return employee.photoUrl
      || this.employeePhotos[employee.id]
      || this.zktecoPhotoCache[this.photoCacheKey(employee)]
      || this.zktecoFacePhotoFor(employee);
  }

  isManualPhoto(employee: Employee): boolean {
    return Boolean(employee.photoUrl || this.employeePhotos[employee.id]);
  }

  photoAdjustmentFor(employee: Employee): PhotoAdjustment {
    if (!this.isManualPhoto(employee)) {
      return { x: 50, y: 50, zoom: 1 };
    }

    const adjustment = this.employeePhotoAdjustments[employee.id] ?? { x: 50, y: 50, zoom: 1.2 };
    return { ...adjustment, zoom: Math.max(1.2, adjustment.zoom) };
  }

  photoPositionFor(employee: Employee): string {
    const adjustment = this.photoAdjustmentFor(employee);
    return `${adjustment.x}% ${adjustment.y}%`;
  }

  photoSizeFor(employee: Employee): string {
    if (!this.isManualPhoto(employee)) {
      return 'cover';
    }

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
    const savedZktecoPhotoCache = localStorage.getItem(this.zktecoPhotoCacheStorageKey);
    const savedPhotoAdjustments = localStorage.getItem(this.employeePhotoAdjustmentsStorageKey);

    if (token) {
      this.isAuthenticated = true;
      this.signedInUser = 'Авторизован';
      this.currentUserRoles = JSON.parse(localStorage.getItem('userRoles') || '[]');
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
    if (savedZktecoPhotoCache) {
      this.zktecoPhotoCache = JSON.parse(savedZktecoPhotoCache);
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
      if (this.activeSection === 'employees' || this.activeSection === 'orgchart' || this.activeSection === 'departments') {
        void this.loadAttendance();
        void this.loadZktecoPersonPhotos();
        void this.ensureEmployeesLoadedFromAd();
      }
    }

    if (this.isAuthenticated && this.activeSection === 'attendance') {
      void this.loadAttendance();
    }
  }

  ngOnDestroy(): void {
    this.stopFaceCamera();
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
    if (section === 'employees' || section === 'orgchart' || section === 'departments') {
      void this.loadAttendance();
      void this.loadZktecoPersonPhotos();
      void this.ensureEmployeesLoadedFromAd();
    }
    if (section === 'settings' && this.isAdminUser) {
      void this.loadAdSettingsPreview();
      void this.loadSettingsTabData();
    }
  }

  setSettingsTab(tab: SettingsTab): void {
    this.activeSettingsTab = tab;
    this.selectedRegistrationRequest = null;
    this.actionMessage = '';
    void this.loadSettingsTabData();
  }

  private async loadSettingsTabData(): Promise<void> {
    if (this.activeSettingsTab === 'requests') {
      await this.loadRegistrationRequests();
    }
    if (this.activeSettingsTab === 'users') {
      await this.loadAdminUsers();
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
      this.currentUserRoles = payload.data.user.roles || [];
      localStorage.setItem('userRoles', JSON.stringify(this.currentUserRoles));
      this.showAuthPanel = false;
      this.actionMessage = 'Вход выполнен, JWT-токен сохранен.';
      if (this.activeSection === 'employees' || this.activeSection === 'orgchart' || this.activeSection === 'departments') {
        void this.ensureEmployeesLoadedFromAd();
      }
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
      this.currentUserRoles = payload.data.user.roles || [];
      localStorage.setItem('userRoles', JSON.stringify(this.currentUserRoles));
      this.showAuthPanel = false;
      this.actionMessage = 'AD-вход выполнен, JWT-токен сохранен.';
      if (this.activeSection === 'employees' || this.activeSection === 'orgchart' || this.activeSection === 'departments') {
        void this.ensureEmployeesLoadedFromAd();
      }
    } catch (error) {
      this.actionMessage = `AD-вход не выполнен: ${error instanceof Error ? error.message : 'ошибка подключения'}`;
    }
  }

  signOut(): void {
    this.isAuthenticated = false;
    this.signedInUser = 'Гость';
    this.currentUserRoles = [];
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userRoles');
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
    this.currentUserRoles = normalizedEmail === 'employee@demo.com' ? ['EMPLOYEE'] : ['HR_ADMIN'];
    localStorage.setItem('userRoles', JSON.stringify(this.currentUserRoles));
    this.showAuthPanel = false;
    this.actionMessage = 'Вход выполнен в локальном демо-режиме. Backend сейчас не запущен.';
    if (this.activeSection === 'employees' || this.activeSection === 'orgchart' || this.activeSection === 'departments') {
      void this.ensureEmployeesLoadedFromAd();
    }
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

  async loadAdEmployees(url: string, baseDn: string, bindDn: string, bindPassword: string, userFilter: string, groupFilter: string, searchBaseDns: string[], navigateToEmployees = true, showMessage = true): Promise<boolean> {
    try {
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
      if (showMessage) {
        this.actionMessage = `Загружено сотрудников из AD: ${this.employees.length}`;
      }
      if (navigateToEmployees) {
        this.setSection('employees');
      }
      return true;
    } catch (error) {
      this.actionMessage = `Сотрудники AD не загружены: ${error instanceof Error ? error.message : 'ошибка подключения'}`;
      return false;
    }
  }

  async syncAdPhotosToZkteco(url: string, baseDn: string, bindDn: string, bindPassword: string, userFilter: string, groupFilter: string, searchBaseDns: string[], showMessage = true): Promise<boolean> {
    try {
      const staffSearchBaseDns = this.staffSearchBases(searchBaseDns);
      const response = await fetch(`${this.apiBaseUrl}/admin/integrations/ad/photos/sync-to-zkteco`, {
        method: 'POST',
        headers: { ...this.authorizedHeaders(), 'Content-Type': 'application/json' },
        body: JSON.stringify({ url, baseDn, bindDn, bindPassword, userFilter, groupFilter, searchBaseDns: staffSearchBaseDns })
      });
      const payload = await response.json();

      if (!response.ok || !payload.success) {
        throw new Error(payload.message || 'photo sync failed');
      }

      const rows = payload.data || [];
      const success = rows.filter((row: any) => row.status === 'SUCCESS').length;
      const skipped = rows.filter((row: any) => row.status === 'SKIPPED').length;
      const errors = rows.filter((row: any) => row.status === 'ERROR').length;
      if (showMessage) {
        this.actionMessage = `AD photos sync finished: ${success} updated, ${skipped} skipped, ${errors} errors.`;
      }
      return errors === 0;
    } catch (error) {
      this.actionMessage = `AD photos were not synced to ZKT: ${error instanceof Error ? error.message : 'connection error'}`;
      return false;
    }
  }

  private async refreshAdEmployeesAndZktPhotos(reason: string): Promise<void> {
    await this.loadAdSettingsPreview();
    const url = this.adSettings.url;
    const baseDn = this.adSettings.baseDn;
    const bindDn = this.adSettings.bindDn;
    const userFilter = this.adSettings.userFilter;
    const groupFilter = this.adSettings.groupFilter;
    const searchBaseDns = this.adSettings.searchBaseDns;

    this.actionMessage = `${reason}. Обновляю сотрудников из AD и фото ZKT...`;
    const employeesLoaded = await this.loadAdEmployees(url, baseDn, bindDn, '', userFilter, groupFilter, searchBaseDns, false, false);
    if (!employeesLoaded) {
      return;
    }
    const photosSynced = await this.syncAdPhotosToZkteco(url, baseDn, bindDn, '', userFilter, groupFilter, searchBaseDns, false);
    this.actionMessage = photosSynced
      ? `${reason}. Сотрудники обновлены из AD, фото отправлены в ZKT.`
      : `${reason}. Сотрудники обновлены из AD, но часть фото ZKT не обновилась.`;
  }

  private async ensureEmployeesLoadedFromAd(): Promise<void> {
    if (this.employees.length || this.employeesAutoLoadInProgress || !this.isAuthenticated) {
      return;
    }

    this.employeesAutoLoadInProgress = true;
    this.actionMessage = 'Список сотрудников пустой. Загружаю сотрудников из AD...';
    try {
      await this.loadAdSettingsPreview();
      const loaded = await this.loadAdEmployees(
        this.adSettings.url,
        this.adSettings.baseDn,
        this.adSettings.bindDn,
        '',
        this.adSettings.userFilter,
        this.adSettings.groupFilter,
        this.adSettings.searchBaseDns,
        false,
        false
      );

      if (loaded) {
        await this.loadZktecoPersonPhotos();
        this.actionMessage = `Сотрудники загружены из AD: ${this.employees.length}.`;
      }
    } finally {
      this.employeesAutoLoadInProgress = false;
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

  async createRegistrationLink(): Promise<void> {
    try {
      const response = await fetch(`${this.apiBaseUrl}/admin/registration-links`, {
        method: 'POST',
        headers: this.authorizedHeaders(false)
      });
      const payload = await response.json();

      if (!response.ok || !payload.success) {
        throw new Error(payload.message || 'link failed');
      }

      this.registrationLink = payload.data.url;
      this.actionMessage = 'Ссылка регистрации создана. Отправьте ее пользователю.';
    } catch (error) {
      this.actionMessage = `Ссылка не создана: ${error instanceof Error ? error.message : 'ошибка подключения'}`;
    }
  }

  setPhoneCountry(code: string): void {
    this.selectedPhoneCountryCode = code;
  }

  digitsOnly(event: Event): string {
    const input = event.target as HTMLInputElement;
    input.value = this.onlyDigits(input.value).slice(0, this.selectedPhoneCountry.maxLength);
    return input.value;
  }

  async startFaceCamera(video: HTMLVideoElement, canvas: HTMLCanvasElement): Promise<void> {
    if (!this.faceCameraSupported) {
      this.faceCaptureStatus = 'invalid';
      this.faceCaptureMessage = 'Браузер не дал доступ к камере. Откройте ссылку через HTTPS или разрешите камеру.';
      return;
    }

    this.stopFaceCamera();
    try {
      this.faceCaptureStatus = 'checking';
      this.faceCaptureMessage = 'Включаем камеру...';
      this.faceCameraStream = await navigator.mediaDevices.getUserMedia({
        video: {
          facingMode: 'user',
          width: { ideal: 900 },
          height: { ideal: 1200 }
        },
        audio: false
      });
      video.srcObject = this.faceCameraStream;
      await video.play();
      this.faceCameraActive = true;
      this.faceCaptureMessage = 'Смотрите прямо в камеру. Лицо должно быть внутри овала.';
      this.faceValidationTimer = window.setInterval(() => void this.validateFaceFrame(video, canvas), 650);
      void this.validateFaceFrame(video, canvas);
    } catch (error) {
      this.faceCaptureStatus = 'invalid';
      this.faceCameraActive = false;
      this.faceCaptureMessage = 'Камера не открылась. Разрешите доступ к камере и попробуйте снова.';
    }
  }

  stopFaceCamera(): void {
    if (this.faceValidationTimer !== null) {
      window.clearInterval(this.faceValidationTimer);
      this.faceValidationTimer = null;
    }
    if (this.faceCameraStream) {
      this.faceCameraStream.getTracks().forEach((track) => track.stop());
      this.faceCameraStream = null;
    }
    this.faceCameraActive = false;
  }

  async captureFacePhoto(video: HTMLVideoElement, canvas: HTMLCanvasElement): Promise<void> {
    const check = await this.analyzeFaceFrame(video, canvas);
    if (!check.ok) {
      this.faceCaptureStatus = 'invalid';
      this.faceCaptureMessage = check.message;
      return;
    }

    canvas.toBlob((blob) => {
      if (!blob) {
        this.faceCaptureStatus = 'invalid';
        this.faceCaptureMessage = 'Фото не сохранилось. Попробуйте еще раз.';
        return;
      }
      this.capturedFaceFile = new File([blob], `face-id-${Date.now()}.jpg`, { type: 'image/jpeg' });
      this.faceCapturePreviewUrl = URL.createObjectURL(blob);
      this.faceCaptureStatus = 'captured';
      this.faceCaptureMessage = 'Фото Face ID принято.';
      this.stopFaceCamera();
    }, 'image/jpeg', 0.92);
  }

  clearCapturedFacePhoto(): void {
    this.capturedFaceFile = null;
    if (this.faceCapturePreviewUrl) {
      URL.revokeObjectURL(this.faceCapturePreviewUrl);
    }
    this.faceCapturePreviewUrl = '';
    this.faceCaptureStatus = 'idle';
    this.faceCaptureMessage = 'Откройте камеру и поместите лицо внутри овала.';
  }

  handleFaceFileSelected(files: FileList | null): void {
    const file = files?.item(0);
    if (!file) {
      return;
    }
    this.capturedFaceFile = null;
    if (this.faceCapturePreviewUrl) {
      URL.revokeObjectURL(this.faceCapturePreviewUrl);
    }
    this.faceCapturePreviewUrl = URL.createObjectURL(file);
    this.faceCaptureStatus = 'captured';
    this.faceCaptureMessage = 'Файл Face ID выбран.';
  }

  private async validateFaceFrame(video: HTMLVideoElement, canvas: HTMLCanvasElement): Promise<void> {
    if (!this.faceCameraActive) {
      return;
    }
    const result = await this.analyzeFaceFrame(video, canvas);
    this.faceCaptureStatus = result.ok ? 'valid' : 'invalid';
    this.faceCaptureMessage = result.message;
  }

  private async analyzeFaceFrame(video: HTMLVideoElement, canvas: HTMLCanvasElement): Promise<{ ok: boolean; message: string }> {
    if (!video.videoWidth || !video.videoHeight) {
      return { ok: false, message: 'Камера еще запускается...' };
    }

    const width = video.videoWidth;
    const height = video.videoHeight;
    canvas.width = width;
    canvas.height = height;
    const context = canvas.getContext('2d', { willReadFrequently: true });
    if (!context) {
      return { ok: false, message: 'Камера недоступна.' };
    }
    context.drawImage(video, 0, 0, width, height);

    const brightness = this.averageFrameBrightness(context, width, height);
    if (brightness < 60) {
      return { ok: false, message: 'Слишком темно. Встаньте ближе к свету.' };
    }
    if (brightness > 225) {
      return { ok: false, message: 'Слишком ярко. Уберите сильный свет с лица.' };
    }

    const Detector = (window as any).FaceDetector;
    if (Detector) {
      try {
        const detector = new Detector({ fastMode: true, maxDetectedFaces: 2 });
        const faces = await detector.detect(canvas);
        if (faces.length !== 1) {
          return { ok: false, message: faces.length > 1 ? 'В кадре должен быть только один человек.' : 'Лицо не найдено. Посмотрите прямо в камеру.' };
        }
        const box = faces[0].boundingBox as DOMRectReadOnly;
        const centerX = box.x + box.width / 2;
        const centerY = box.y + box.height / 2;
        const horizontalOk = centerX > width * 0.28 && centerX < width * 0.72;
        const verticalOk = centerY > height * 0.24 && centerY < height * 0.68;
        const sizeOk = box.width > width * 0.22 && box.width < width * 0.68 && box.height > height * 0.22 && box.height < height * 0.78;
        if (!horizontalOk || !verticalOk) {
          return { ok: false, message: 'Поставьте лицо по центру овала.' };
        }
        if (!sizeOk) {
          return { ok: false, message: 'Подойдите ближе или чуть дальше, чтобы лицо попало в овал.' };
        }
        return { ok: true, message: 'Отлично. Лицо в кадре, можно сделать фото.' };
      } catch (error) {
        // Browser has FaceDetector but refused this frame. Fall back to light/position guide.
      }
    }

    return { ok: true, message: 'Свет хороший. Держите лицо внутри овала и сделайте фото.' };
  }

  private averageFrameBrightness(context: CanvasRenderingContext2D, width: number, height: number): number {
    const sampleWidth = Math.max(1, Math.floor(width / 8));
    const sampleHeight = Math.max(1, Math.floor(height / 8));
    const startX = Math.floor((width - sampleWidth) / 2);
    const startY = Math.floor((height - sampleHeight) / 2);
    const data = context.getImageData(startX, startY, sampleWidth, sampleHeight).data;
    let total = 0;
    for (let index = 0; index < data.length; index += 4) {
      total += (data[index] + data[index + 1] + data[index + 2]) / 3;
    }
    return total / (data.length / 4);
  }

  private onlyDigits(value: string): string {
    return (value || '').replace(/\D/g, '');
  }

  async submitRegistration(firstName: string, lastName: string, department: string, phoneDigits: string, personalEmail: string, identityFiles: FileList | null, faceFiles: FileList | null): Promise<void> {
    if (this.registrationSubmitting || this.registrationSubmitted) {
      return;
    }

    const digits = this.onlyDigits(phoneDigits);
    const cleanFirstName = firstName.trim();
    const cleanLastName = lastName.trim();
    const fullName = `${cleanFirstName} ${cleanLastName}`.trim();
    const selectedDepartment = this.cleanDepartmentName(department);
    const phone = `${this.selectedPhoneCountry.dialCode}${digits}`;
    const identityFile = identityFiles?.item(0);
    const faceFile = this.capturedFaceFile || faceFiles?.item(0);
    if (!cleanFirstName || !cleanLastName || !selectedDepartment || !digits || !personalEmail || !identityFile || !faceFile) {
      this.actionMessage = 'Заполните все обязательные поля перед отправкой заявки.';
      return;
    }
    if (digits.length < this.selectedPhoneCountry.minLength || digits.length > this.selectedPhoneCountry.maxLength) {
      this.actionMessage = `Введите ${this.selectedPhoneCountry.minLength === this.selectedPhoneCountry.maxLength ? this.selectedPhoneCountry.minLength : `${this.selectedPhoneCountry.minLength}-${this.selectedPhoneCountry.maxLength}`} цифр номера для страны ${this.selectedPhoneCountry.name}.`;
      return;
    }

    try {
      this.registrationSubmitting = true;
      this.actionMessage = 'Загружаем файлы регистрации...';
      const identityDocumentFileId = await this.uploadRegistrationFile('identity', identityFile);
      const facePhotoFileId = await this.uploadRegistrationFile('face', faceFile);
      const response = await fetch(`${this.apiBaseUrl}/registration/${this.registrationToken}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ firstName: cleanFirstName, lastName: cleanLastName, fullName, department: selectedDepartment, phone, personalEmail, identityDocumentFileId, facePhotoFileId })
      });
      const payload = await response.json();

      if (!response.ok || !payload.success) {
        throw new Error(payload.message || 'registration failed');
      }

      this.registrationSubmitted = true;
      this.actionMessage = 'Заявка отправлена администратору.';
    } catch (error) {
      this.actionMessage = `Заявка не отправлена: ${error instanceof Error ? error.message : 'ошибка подключения'}`;
    } finally {
      this.registrationSubmitting = false;
    }
  }

  private async uploadRegistrationFile(type: 'identity' | 'face', file: File): Promise<string> {
    const formData = new FormData();
    formData.append('type', type);
    formData.append('file', file);

    const response = await fetch(`${this.apiBaseUrl}/registration/${this.registrationToken}/files`, {
      method: 'POST',
      body: formData
    });
    const payload = await response.json();

    if (!response.ok || !payload.success) {
      throw new Error(payload.message || 'file upload failed');
    }

    return payload.data.id;
  }

  handlePrimaryAction(): void {
    this.actionMessage = this.selectedEmployee
      ? `Открыта форма редактирования сотрудника: ${this.selectedEmployee.name}.`
      : `Действие для раздела "${this.sectionTitles[this.activeSection]}" готово к подключению к базе данных.`;
  }

  openReport(name: string): void {
    this.actionMessage = `Отчет "${name}" открыт в демо-режиме.`;
  }

  openEmployeeEdit(): void {
    if (!this.selectedEmployee) {
      return;
    }
    this.employeeEditDraft = { ...this.selectedEmployee };
    this.actionMessage = '';
  }

  cancelEmployeeEdit(): void {
    this.employeeEditDraft = null;
  }

  async saveEmployeeEdit(name: string, email: string, phone: string, department: string, position: string, status: string): Promise<void> {
    if (!this.selectedEmployee) {
      return;
    }
    const updated: Employee = {
      ...this.selectedEmployee,
      name: name.trim() || this.selectedEmployee.name,
      email: email.trim(),
      phone: phone.trim(),
      department: department.trim() || 'Без отдела',
      position: position.trim() || 'Сотрудник',
      status: this.toEmployeeStatus(status),
      statusLabel: this.statusLabel(status)
    };
    this.employees = this.employees.map((employee) => employee.id === updated.id ? updated : employee);
    localStorage.setItem(this.adEmployeesStorageKey, JSON.stringify(this.employees));
    this.employeeEditDraft = null;
    this.actionMessage = `Данные сотрудника сохранены: ${updated.name}.`;
    await this.refreshAdEmployeesAndZktPhotos(`Данные сотрудника сохранены: ${updated.name}`);
  }

  private toEmployeeStatus(status: string): Employee['status'] {
    return ['active', 'remote', 'leave'].includes(status) ? status as Employee['status'] : 'active';
  }

  private statusLabel(status: string): string {
    return status === 'remote' ? 'Удаленно' : status === 'leave' ? 'Отпуск' : 'Активен';
  }

  startEmployeeDrag(event: DragEvent, employee: Employee): void {
    if (!this.isAdminUser) {
      return;
    }
    this.draggedEmployeeId = employee.id;
    event.dataTransfer?.setData('text/plain', String(employee.id));
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move';
    }
  }

  allowDepartmentDrop(event: DragEvent): void {
    if (!this.isAdminUser || this.draggedEmployeeId === null) {
      return;
    }
    event.preventDefault();
    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = 'move';
    }
  }

  async dropEmployeeToDepartment(event: DragEvent, departmentName: string): Promise<void> {
    event.preventDefault();
    const employeeId = Number(event.dataTransfer?.getData('text/plain') || this.draggedEmployeeId);
    this.draggedEmployeeId = null;
    if (!Number.isFinite(employeeId)) {
      return;
    }
    await this.moveEmployeeToDepartment(employeeId, departmentName);
  }

  clearEmployeeDrag(): void {
    this.draggedEmployeeId = null;
  }

  async loadAdminUsers(): Promise<void> {
    try {
      const response = await fetch(`${this.apiBaseUrl}/admin/users`, {
        headers: this.authorizedHeaders(false)
      });
      const payload = await response.json();
      if (!response.ok || !payload.success) {
        throw new Error(payload.message || 'users unavailable');
      }
      this.adminUsers = payload.data ?? [];
    } catch (error) {
      this.actionMessage = `Пользователи не загружены: ${error instanceof Error ? error.message : 'ошибка подключения'}`;
    }
  }

  async setUserBlocked(user: AdminUserItem, blocked: boolean): Promise<void> {
    const previousUsers = [...this.adminUsers];
    this.adminUsers = this.adminUsers.map((item) =>
      item.id === user.id ? { ...item, status: blocked ? 'BLOCKED' : 'ACTIVE' } : item
    );
    try {
      const response = await fetch(`${this.apiBaseUrl}/admin/users/${user.id}/${blocked ? 'block' : 'unblock'}`, {
        method: 'POST',
        headers: this.authorizedHeaders(false)
      });
      const payload = await response.json();
      if (!response.ok || !payload.success) {
        throw new Error(payload.message || 'status update failed');
      }
      this.adminUsers = this.adminUsers.map((item) => item.id === user.id ? payload.data : item);
      this.actionMessage = blocked ? `Пользователь ${user.email} заблокирован.` : `Пользователь ${user.email} разблокирован.`;
    } catch (error) {
      this.adminUsers = previousUsers;
      this.actionMessage = `Статус пользователя не изменен: ${error instanceof Error ? error.message : 'ошибка подключения'}`;
    }
  }

  private async moveEmployeeToDepartment(employeeId: number, departmentName: string): Promise<void> {
    const employee = this.employees.find((candidate) => candidate.id === employeeId);
    if (!employee) {
      return;
    }

    const targetDepartment = this.cleanDepartmentName(departmentName);
    if (this.cleanDepartmentName(employee.department) === targetDepartment) {
      this.actionMessage = `${employee.name} уже находится в отделе ${targetDepartment}.`;
      return;
    }

    const previousEmployees = [...this.employees];
    const targetGroup = this.departmentGroups.find((group) => group.name === targetDepartment);
    const manager = this.findDepartmentLeader((targetGroup?.employees || []).filter((candidate) => candidate.id !== employee.id));
    const managerLogin = manager ? (manager.adUsername || this.loginForEmployee(manager)) : '';
    const managerName = manager?.name || '';
    const updatedEmployee: Employee = {
      ...employee,
      department: targetDepartment,
      rootDepartment: targetDepartment,
      manager: managerName
    };

    this.employees = this.employees
      .map((candidate) => candidate.id === employee.id ? updatedEmployee : candidate)
      .sort((a, b) => a.department.localeCompare(b.department, 'ru-RU') || a.name.localeCompare(b.name, 'ru-RU'));
    localStorage.setItem(this.adEmployeesStorageKey, JSON.stringify(this.employees));
    this.actionMessage = `${employee.name} перенесен в ${targetDepartment}. Отправляем изменение в AD...`;

    try {
      const response = await fetch(`${this.apiBaseUrl}/admin/integrations/ad/users/move`, {
        method: 'POST',
        headers: this.authorizedHeaders(),
        body: JSON.stringify({
          login: employee.adUsername || this.loginForEmployee(employee),
          department: targetDepartment,
          managerLogin
        })
      });
      const payload = await response.json();
      if (!response.ok || !payload.success) {
        throw new Error(payload.message || 'AD update failed');
      }
      const status = payload.data?.status || 'OK';
      this.actionMessage = `${employee.name}: отдел изменен на ${targetDepartment}, руководитель ${managerName || 'не назначен'}. AD: ${status}.`;
      await this.refreshAdEmployeesAndZktPhotos(`${employee.name}: отдел изменен на ${targetDepartment}`);
    } catch (error) {
      this.employees = previousEmployees;
      localStorage.setItem(this.adEmployeesStorageKey, JSON.stringify(this.employees));
      this.actionMessage = `Перенос отменен: AD не обновился (${error instanceof Error ? error.message : 'ошибка подключения'}).`;
    }
  }

  async loadRegistrationRequests(): Promise<void> {
    try {
      const response = await fetch(`${this.apiBaseUrl}/admin/registration-requests`, {
        headers: this.authorizedHeaders(false)
      });
      const payload = await response.json();
      if (!response.ok || !payload.success) {
        throw new Error(payload.message || 'requests unavailable');
      }
      this.registrationRequests = payload.data ?? [];
      this.actionMessage = `Заявок загружено: ${this.registrationRequests.length}`;
    } catch (error) {
      this.actionMessage = `Заявки не загружены: ${error instanceof Error ? error.message : 'ошибка подключения'}`;
    }
  }

  openRegistrationRequest(request: RegistrationRequestItem): void {
    this.selectedRegistrationRequest = request;
  }

  closeRegistrationRequest(): void {
    this.selectedRegistrationRequest = null;
  }

  canProcessRegistrationRequest(request: RegistrationRequestItem): boolean {
    return !['COMPLETED', 'REJECTED'].includes(request.status);
  }

  registrationFileUrl(fileId: string): string {
    return `${this.apiBaseUrl}/admin/registration-files/${fileId}`;
  }

  async openRegistrationFile(fileId: string): Promise<void> {
    try {
      const response = await fetch(this.registrationFileUrl(fileId), {
        headers: this.authorizedHeaders(false)
      });
      if (!response.ok) {
        throw new Error('file unavailable');
      }
      const blob = await response.blob();
      window.open(URL.createObjectURL(blob), '_blank', 'noopener');
    } catch (error) {
      this.actionMessage = `Файл не открыт: ${error instanceof Error ? error.message : 'ошибка подключения'}`;
    }
  }

  async approveRegistrationRequest(id: string, department: string): Promise<void> {
    const processed = await this.updateRegistrationRequest(id, 'approve', undefined, department);
    if (processed) {
      await this.refreshAdEmployeesAndZktPhotos('Заявка одобрена');
    }
  }

  async rejectRegistrationRequest(id: string): Promise<void> {
    const reason = window.prompt('Причина отклонения заявки') || 'Отклонено администратором';
    await this.updateRegistrationRequest(id, 'reject', reason);
  }

  private async updateRegistrationRequest(id: string, action: 'approve' | 'reject', reason?: string, department?: string): Promise<boolean> {
    try {
      const endpoint = action === 'approve' && department ? 'approve-with-department' : action;
      const response = await fetch(`${this.apiBaseUrl}/admin/registration-requests/${id}/${endpoint}`, {
        method: 'POST',
        headers: this.authorizedHeaders(),
        body: action === 'reject' ? JSON.stringify({ reason }) : action === 'approve' ? JSON.stringify({ department }) : undefined
      });
      const payload = await response.json();
      if (!response.ok || !payload.success) {
        throw new Error(payload.message || `${action} failed`);
      }
      this.actionMessage = payload.message || 'Заявка обработана.';
      await this.loadRegistrationRequests();
      this.selectedRegistrationRequest = null;
      return true;
    } catch (error) {
      this.actionMessage = `Заявка не обработана: ${error instanceof Error ? error.message : 'ошибка подключения'}`;
      return false;
    }
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
      void this.preloadAttendancePhotos(this.attendanceEmployees);
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
      ? `${this.apiBaseUrl}/attendance/photo?path=${encodeURIComponent(employee.photoPath)}&v=faceid`
      : '';
  }

  attendancePhotoSrc(employee: LiveAttendanceEmployee): string | undefined {
    if (!employee.photoPath) {
      return undefined;
    }
    return this.attendancePhotoDataUrls[this.attendancePhotoKey(employee.photoPath)];
  }

  private async preloadAttendancePhotos(employees: LiveAttendanceEmployee[]): Promise<void> {
    const paths = Array.from(new Set(
      employees
        .filter((employee) => this.isKnownAdAttendance(employee))
        .map((employee) => employee.photoPath)
        .filter((path): path is string => Boolean(path))
    )).slice(0, 120);

    for (const path of paths) {
      const key = this.attendancePhotoKey(path);
      if (this.attendancePhotoDataUrls[key] || this.attendancePhotoLoadFailures.has(key)) {
        continue;
      }

      try {
        const response = await fetch(`${this.apiBaseUrl}/attendance/photo?path=${encodeURIComponent(path)}&v=${Date.now()}`, {
          headers: this.authorizedHeaders(false)
        });
        if (!response.ok) {
          this.attendancePhotoLoadFailures.add(key);
          continue;
        }

        const blob = await response.blob();
        if (!blob.type.startsWith('image/')) {
          this.attendancePhotoLoadFailures.add(key);
          continue;
        }

        this.attendancePhotoDataUrls[key] = await this.blobToDataUrl(blob);
        this.groupedAttendanceEmployees = [...this.groupedAttendanceEmployees];
      } catch (error) {
        this.attendancePhotoLoadFailures.add(key);
      }
    }
  }

  private attendancePhotoKey(path: string): string {
    return path.replace(/\\/g, '/').toLowerCase();
  }

  zktecoFacePhotoFor(employee: Employee): string | undefined {
    const directoryPhoto = this.zktecoPhotoForEmployee(employee);
    if (directoryPhoto) {
      return directoryPhoto;
    }
    const match = this.attendanceForEmployee(employee);
    const url = match ? this.attendancePhotoUrl(match) : '';
    return url && !this.brokenPhotoUrls.has(url) ? url : undefined;
  }

  async loadZktecoPersonPhotos(): Promise<void> {
    try {
      const response = await fetch(`${this.apiBaseUrl}/attendance/photos`, {
        headers: this.authorizedHeaders(false)
      });
      const payload = await response.json();
      if (!response.ok || !payload.success) {
        throw new Error(payload.message || 'photos unavailable');
      }
      this.zktecoPersonPhotos = payload.data ?? [];
    } catch (error) {
      // Cached photos and initials remain visible if ZKT photo directory is temporarily unavailable.
    }
  }

  private zktecoPhotoForEmployee(employee: Employee): string | undefined {
    const login = this.loginForEmployee(employee).toLowerCase();
    const adUsername = (employee.adUsername || '').toLowerCase();
    const fullName = employee.name.trim().toLowerCase();
    const match = this.zktecoPersonPhotos.find((candidate) => {
      const pin = (candidate.pin || '').toLowerCase();
      const candidateName = `${candidate.name || ''} ${candidate.lastName || ''}`.trim().toLowerCase();
      return Boolean(candidate.photoPath)
        && (pin === login || pin === adUsername || candidateName === fullName);
    });
    if (!match?.photoPath) {
      return undefined;
    }
    const url = `${this.apiBaseUrl}/attendance/photo?path=${encodeURIComponent(match.photoPath)}&v=zkt-directory`;
    return this.brokenPhotoUrls.has(url) ? undefined : url;
  }

  async cacheEmployeePhoto(employee: Employee, url: string): Promise<void> {
    if (!url || url.startsWith('data:') || this.zktecoPhotoCache[this.photoCacheKey(employee)]) {
      return;
    }
    try {
      const response = await fetch(url);
      if (!response.ok) {
        return;
      }
      const blob = await response.blob();
      if (!blob.type.startsWith('image/') || blob.size > 350_000) {
        return;
      }
      const dataUrl = await this.blobToDataUrl(blob);
      this.zktecoPhotoCache[this.photoCacheKey(employee)] = dataUrl;
      this.trimPhotoCache();
      localStorage.setItem(this.zktecoPhotoCacheStorageKey, JSON.stringify(this.zktecoPhotoCache));
    } catch (error) {
      // The visible image already loaded; cache is only a resilience layer.
    }
  }

  markEmployeePhotoBroken(employee: Employee, url: string): void {
    if (url) {
      this.brokenPhotoUrls.add(url);
    }
    if (!this.zktecoPhotoCache[this.photoCacheKey(employee)]) {
      this.employees = [...this.employees];
    }
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

    return this.attendanceForEmployee(employee);
  }

  private attendanceForEmployee(employee: Employee): LiveAttendanceEmployee | undefined {
    const login = this.loginForEmployee(employee).toLowerCase();
    const pin = (employee.zktecoPin || employee.adUsername || login).toLowerCase();
    const employeeName = employee.name.toLowerCase();

    return this.attendanceEmployees.find((candidate) =>
      Boolean(candidate.photoPath)
      && (
        (candidate.pin || '').toLowerCase() === pin
        || `${candidate.name || ''} ${candidate.lastName || ''}`.trim().toLowerCase() === employeeName
      )
    ) || this.attendanceEmployees.find((candidate) =>
      (candidate.pin || '').toLowerCase() === pin
      || `${candidate.name || ''} ${candidate.lastName || ''}`.trim().toLowerCase() === employeeName
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
    const registrationMatch = cleanPath.match(/^\/registration\/([a-f0-9]+)$/i);

    if (registrationMatch) {
      this.activeSection = 'registration';
      this.registrationToken = registrationMatch[1];
      this.selectedEmployeeId = null;
      return;
    }

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
        bindDn: payload.data.bindDn || this.adSettings.bindDn,
        userFilter: payload.data.userFilter || this.adSettings.userFilter,
        groupFilter: payload.data.groupFilter || this.adSettings.groupFilter,
        searchBaseDns: this.staffSearchBases(payload.data.searchBaseDns)
      };
      this.adSettings.bindPassword = '';
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

  private photoCacheKey(employee: Employee): string {
    return (employee.adUsername || this.loginForEmployee(employee) || String(employee.id)).toLowerCase();
  }

  private blobToDataUrl(blob: Blob): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(String(reader.result || ''));
      reader.onerror = () => reject(reader.error);
      reader.readAsDataURL(blob);
    });
  }

  private trimPhotoCache(): void {
    const entries = Object.entries(this.zktecoPhotoCache);
    const maxEntries = 80;
    if (entries.length <= maxEntries) {
      return;
    }
    this.zktecoPhotoCache = Object.fromEntries(entries.slice(entries.length - maxEntries));
  }

  rememberAdCredentials(bindDn: string, bindPassword: string): void {
    this.adSettings.bindDn = bindDn;
    this.adSettings.bindPassword = bindPassword;
    sessionStorage.setItem(this.adBindDnSessionKey, bindDn);
    sessionStorage.setItem(this.adBindPasswordSessionKey, bindPassword);
  }
}
