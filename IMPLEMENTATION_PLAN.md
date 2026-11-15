# Implementation Plan - Connect Oil Web Platform

## Excluded: US13 (Mobile App) - Not viable for web-only

## Phase 1: Core Authentication & Registration (US01, US08)
### Backend
- [ ] Enhance User entity: add CNPJ field, password complexity validation
- [ ] Add Account lockout after 5 failed attempts
- [ ] Implement password hashing (BCrypt)
- [ ] Add unique constraints on email and CNPJ/CPF
- [ ] Create Account Deletion endpoint
- [ ] Add JWT token blacklist for logout
- [ ] Implement HTTPS enforcement config

### Frontend
- [ ] Update Registration form: CNPJ/CPF field with validation
- [ ] Add password strength indicator
- [ ] Add Account Deletion in Profile page
- [ ] Improve error messages for duplicate email/CNPJ

## Phase 2: Collection Scheduling (US02, US03, US11, US12)
### Backend
- [ ] Create CollectionSchedule entity enhancements: time windows, sale/exchange type
- [ ] Add validation: future dates only, volume > 0
- [ ] Implement status transitions: SCHEDULED → ASSIGNED → EN_ROUTE → COMPLETED/CANCELLED
- [ ] Add cancellation rules: 2h before for establishment, anytime with justification for collector
- [ ] Create "almost full" alert endpoint
- [ ] Implement notification system (email/push placeholder)

### Frontend
- [ ] Create Schedule Wizard: date picker, time windows (morning/afternoon/flexible), volume, type selector
- [ ] Add "Almost Full" alert button on dashboard
- [ ] Show status timeline with notifications
- [ ] Implement cancellation with confirmation modal

## Phase 3: Collector Dashboard & Acceptance (US04, US12)
### Backend
- [ ] Add collector region/coverage area
- [ ] Create endpoint for available collections with filters (date, region)
- [ ] Implement collection acceptance (atomic operation to prevent race conditions)
- [ ] Add real-time updates via WebSocket or polling

### Frontend
- [ ] Build Collector Dashboard with map/list view
- [ ] Add filters: date range, region, status
- [ ] Implement "Accept Collection" with confirmation
- [ ] Auto-refresh every 3 seconds

## Phase 4: Collection Completion & Weight Registration (US05)
### Backend
- [ ] Create CollectionRecord with actual weight, confirmation code
- [ ] Implement discrepancy detection (estimated vs actual > threshold)
- [ ] Add establishment confirmation (code or signature placeholder)
- [ ] Make records immutable after completion
- [ ] Trigger notifications for discrepancies

### Frontend
- [ ] Build Collection Completion form: actual volume, confirmation code input
- [ ] Show discrepancy warning if > threshold
- [ ] Display read-only completed collections

## Phase 5: Oil Sale/Exchange (US06)
### Backend
- [ ] Create Product entity (cleaning products catalog)
- [ ] Add base oil price configuration
- [ ] Implement Sale vs Exchange calculation logic
- [ ] Create Exchange Order with product quantities
- [ ] Link exchange to collection schedule

### Frontend
- [ ] Add Sale/Exchange toggle in scheduling
- [ ] Show calculated value based on volume
- [ ] Product selection modal for exchange
- [ ] Display products to bring on collector view

## Phase 6: History & PDF Certificates (US07, US14)
### Backend
- [ ] Create history endpoint with filters and totals
- [ ] Generate PDF certificate with verification code (QR/hash)
- [ ] Implement audit logging for all collection changes
- [ ] Add verification endpoint for certificate codes

### Frontend
- [ ] Build History page with filters, totals
- [ ] Add "Download Certificate" button
- [ ] Show verification code on certificate

## Phase 7: Environmental Features (US15)
### Backend
- [ ] Add storage guidelines content
- [ ] Create Incident entity for spills/leaks
- [ ] Add collector validation (documents, vehicle)
- [ ] Prioritize "almost full" in collector list

### Frontend
- [ ] Show storage guidelines modal during scheduling
- [ ] Add Incident Report form
- [ ] Highlight priority collections for collectors

## Phase 8: Non-Functional (US08, US09, US10, US14)
- [ ] Security audit: password hash, RBAC, HTTPS, account lockout
- [ ] Performance: add indexes, optimize queries, add caching
- [ ] Usability: simplify language, add tooltips, reduce clicks
- [ ] Data integrity: audit logs, verification codes, daily backup config

---

## Technical Debt to Address
1. Update User entity: CNPJ, establishment name, address fields
2. Add CollectionPoint status: ALMOST_FULL
3. Add CollectionSchedule: saleType (SALE/EXCHANGE), timeWindow
4. Add CollectionRecord: actualVolume, confirmationCode, discrepancyFlag
5. Add Product catalog
6. Add Incident entity
7. Add AuditLog entity
8. Add Notification entity