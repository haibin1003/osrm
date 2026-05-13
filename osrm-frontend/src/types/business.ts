export interface BusinessSystem {
  id: number;
  systemCode: string;
  systemName: string;
  systemAlias?: string;
  unit?: string;
  category?: string;
  domainL1?: string;
  domainL2?: string;
  domainL3?: string;
  status?: string;
  statusName?: string;
  responsiblePerson?: string;
  responsibleDept?: string;
  description?: string;
  enabled: boolean;
  createdBy?: number;
  createdAt: string;
  updatedAt: string;
}

export interface BusinessSystemForm {
  systemCode: string;
  systemName: string;
  domainL1?: string;
  domainL2?: string;
  domainL3?: string;
  responsiblePerson?: string;
  description?: string;
}
