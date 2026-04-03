export interface Subscription {
  id: number;
  packageId: number;
  packageName: string;
  versionId: number;
  versionNumber: string;
  businessSystemId: number;
  systemName: string;
  useScene: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'REVOKED';
  statusName: string;
  applicantId: number;
  createdAt: string;
}
