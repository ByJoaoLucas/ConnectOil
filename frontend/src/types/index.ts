export interface User {
  id: string;
  name: string;
  email: string;
  cpf?: string;
  cnpj?: string;
  phoneNumber?: string;
  establishmentName?: string;
  establishmentAddress?: string;
  establishmentCity?: string;
  establishmentState?: string;
  establishmentZipCode?: string;
  role: 'ADMIN' | 'COLLECTOR' | 'CLIENT' | 'ESTABLISHMENT';
  isActive: boolean;
  failedLoginAttempts?: number;
  lockedUntil?: string;
  passwordChangedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface RegisterData {
  name: string;
  email: string;
  password: string;
  cpf?: string;
  cnpj?: string;
  phoneNumber?: string;
  establishmentName?: string;
  establishmentAddress?: string;
  establishmentCity?: string;
  establishmentState?: string;
  establishmentZipCode?: string;
  role: 'COLLECTOR' | 'ESTABLISHMENT';
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface CollectionPoint {
  id: string;
  name: string;
  description?: string;
  address: string;
  neighborhood?: string;
  city: string;
  state: string;
  zipCode?: string;
  latitude?: number;
  longitude?: number;
  status: 'ACTIVE' | 'INACTIVE' | 'MAINTENANCE' | 'FULL' | 'PENDING_APPROVAL';
  capacityLiters?: number;
  currentVolumeLiters: number;
  contactName?: string;
  contactPhone?: string;
  contactEmail?: string;
  operatingHours?: string;
  acceptedOilTypes?: string;
  collectorId?: string;
  collectorName?: string;
  fillPercentage: number;
  createdAt?: string;
  updatedAt?: string;
}

export type SaleType = 'SALE' | 'EXCHANGE';

export interface CollectionSchedule {
  id: string;
  collectionPointId: string;
  collectionPointName?: string;
  collectorId: string;
  collectorName?: string;
  scheduledDate: string;
  startTime: string;
  endTime?: string;
  status: 'PENDING' | 'CONFIRMED' | 'ASSIGNED' | 'EN_ROUTE' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | 'RESCHEDULED';
  estimatedVolumeLiters?: number;
  notes?: string;
  // US02, US06 - Sale/Exchange
  saleType?: SaleType;
  timeWindow?: string;
  basePricePerLiter?: number;
  calculatedValue?: number;
  // US03 - Status tracking timestamps
  assignedAt?: string;
  enRouteAt?: string;
  completedAt?: string;
  cancelledAt?: string;
  cancellationReason?: string;
  createdAt?: string;
  updatedAt?: string;
  isOverdue: boolean;
  isToday: boolean;
  // US06 - Exchange items
  exchangeItems?: ExchangeItem[];
}

// US05 - Collection record with new fields
export interface CollectionRecord {
  id: string;
  collectionPointId: string;
  collectionPointName?: string;
  collectorId: string;
  collectorName?: string;
  scheduleId?: string;
  collectionDate: string;
  volumeCollectedLiters: number;
  oilType?: string;
  containerCount?: number;
  containerType?: string;
  qualityRating?: number;
  temperatureCelsius?: number;
  phLevel?: number;
  contaminationPercentage?: number;
  photos?: string;
  notes?: string;
  gpsLatitude?: number;
  gpsLongitude?: number;
  verifiedByManager: boolean;
  verifiedAt?: string;
  verifiedById?: string;
  verifiedByName?: string;
  // US05, US14 - Confirmation and certificate
  confirmationCode?: string;
  establishmentConfirmed?: boolean;
  establishmentConfirmedAt?: string;
  establishmentConfirmedById?: string;
  discrepancyFlag?: boolean;
  discrepancyDetails?: string;
  certificateCode?: string;
  createdAt?: string;
  updatedAt?: string;
  isHighQuality: boolean;
  hasContamination: boolean;
}

export interface PaginatedResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      sorted: boolean;
      unsorted: boolean;
      empty: boolean;
    };
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  totalPages: number;
  totalElements: number;
  last: boolean;
  size: number;
  number: number;
  sort: {
    sorted: boolean;
    unsorted: boolean;
    empty: boolean;
  };
  first: boolean;
  numberOfElements: number;
  empty: boolean;
}

export interface DashboardStatsDTO {
  // Collection Points
  totalPoints: number;
  activePoints: number;
  fullPoints: number;
  pendingApprovalPoints: number;

  // Schedules
  totalSchedules: number;
  pendingSchedules: number;
  confirmedSchedules: number;
  todaySchedules: number;
  overdueSchedules: number;
  inProgressSchedules: number;
  completedSchedules: number;

  // Records
  totalRecords: number;
  pendingVerification: number;
  monthRecords: number;
  totalVolume: number;
  monthVolume: number;

  // Users
  totalCollectors: number;
  activeCollectors: number;
  totalEstablishments: number;
  activeEstablishments: number;

  // Collector specific
  avgQuality: number;
}

export interface DailyVolumeDTO {
  date: string;
  totalVolume: number;
}

export interface CollectorPerformanceDTO {
  collectorId: string;
  collectorName: string;
  totalCollections: number;
  totalVolume: number;
  averageQuality: number;
}

export interface OilTypeVolumeDTO {
  oilType: string;
  totalVolume: number;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  validationErrors?: Record<string, string>;
}

export type PointStatus = 'ACTIVE' | 'INACTIVE' | 'MAINTENANCE' | 'FULL' | 'PENDING_APPROVAL';
export type ScheduleStatus = 'PENDING' | 'CONFIRMED' | 'ASSIGNED' | 'EN_ROUTE' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | 'RESCHEDULED';
export type UserRole = 'ADMIN' | 'COLLECTOR' | 'CLIENT' | 'ESTABLISHMENT';

export const STATUS_LABELS: Record<PointStatus, string> = {
  ACTIVE: 'Ativo',
  INACTIVE: 'Inativo',
  MAINTENANCE: 'Manutenção',
  FULL: 'Cheio',
  PENDING_APPROVAL: 'Pendente Aprovação',
};

export const SCHEDULE_STATUS_LABELS: Record<ScheduleStatus, string> = {
  PENDING: 'Pendente',
  CONFIRMED: 'Confirmado',
  ASSIGNED: 'Atribuído',
  EN_ROUTE: 'A Caminho',
  IN_PROGRESS: 'Em Andamento',
  COMPLETED: 'Concluído',
  CANCELLED: 'Cancelado',
  RESCHEDULED: 'Reagendado',
};

export const USER_ROLE_LABELS: Record<UserRole, string> = {
  ADMIN: 'Administrador',
  COLLECTOR: 'Coletor',
  CLIENT: 'Cliente',
  ESTABLISHMENT: 'Estabelecimento',
};

export const STATUS_COLORS: Record<PointStatus, string> = {
  ACTIVE: 'bg-green-100 text-green-800',
  INACTIVE: 'bg-gray-100 text-gray-800',
  MAINTENANCE: 'bg-yellow-100 text-yellow-800',
  FULL: 'bg-red-100 text-red-800',
  PENDING_APPROVAL: 'bg-blue-100 text-blue-800',
};

export const SCHEDULE_STATUS_COLORS: Record<ScheduleStatus, string> = {
  PENDING: 'bg-gray-100 text-gray-800',
  CONFIRMED: 'bg-blue-100 text-blue-800',
  ASSIGNED: 'bg-indigo-100 text-indigo-800',
  EN_ROUTE: 'bg-orange-100 text-orange-800',
  IN_PROGRESS: 'bg-yellow-100 text-yellow-800',
  COMPLETED: 'bg-green-100 text-green-800',
  CANCELLED: 'bg-red-100 text-red-800',
  RESCHEDULED: 'bg-purple-100 text-purple-800',
};

// US06 - Product for exchange
export interface Product {
  id: string;
  name: string;
  description?: string;
  category: string;
  unit: string;
  pricePerUnit: number;
  stockQuantity: number;
  minStockQuantity: number;
  isActive: boolean;
  imageUrl?: string;
  createdAt?: string;
  updatedAt?: string;
  isInStock: boolean;
  isLowStock: boolean;
}

// US06 - Exchange item
export interface ExchangeItem {
  id: string;
  productId: string;
  productName?: string;
  productCategory?: string;
  productUnit?: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

// US04, US11, US15 - Incident
export interface Incident {
  id: string;
  collectionPointId: string;
  collectionPointName?: string;
  scheduleId?: string;
  reporterId: string;
  reporterName?: string;
  incidentType: string;
  description?: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  photos?: string;
  gpsLatitude?: number;
  gpsLongitude?: number;
  status: 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';
  resolvedAt?: string;
  resolvedById?: string;
  resolvedByName?: string;
  resolutionNotes?: string;
  createdAt?: string;
  updatedAt?: string;
  isOpen: boolean;
  isResolved: boolean;
}

// US03, US11, US12 - Notification
export interface Notification {
  id: string;
  userId: string;
  userName?: string;
  title: string;
  message: string;
  type: string;
  relatedEntityType?: string;
  relatedEntityId?: string;
  deliveryMethod: string;
  isRead: boolean;
  sentAt: string;
  readAt?: string;
}

// US08, US10, US14 - System Config
export interface SystemConfig {
  id: string;
  configKey: string;
  configValue: string;
  description?: string;
  dataType: string;
  createdAt?: string;
  updatedAt?: string;
}

// US04 - Collector Coverage
export interface CollectorCoverage {
  id: string;
  collectorId: string;
  collectorName?: string;
  city: string;
  state: string;
  neighborhoods?: string;
  maxDistanceKm?: number;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
}

// US15 - Storage Guidelines
export interface StorageGuideline {
  id: string;
  title: string;
  content: string;
  category: string;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
}