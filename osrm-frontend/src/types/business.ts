export type BusinessDomain = 'BUSINESS' | 'OPERATION' | 'RESOURCE' | 'SERVICE' | 'DATA';

export interface BusinessSystem {
  id: number;
  systemCode: string;
  systemName: string;
  domain: BusinessDomain;
  domainName: string;
  responsiblePerson?: string;
  description?: string;
  enabled: boolean;
  createdBy?: number;
  createdAt: string;
  updatedAt: string;
}

export interface BusinessSystemForm {
  systemCode: string;
  systemName: string;
  domain: BusinessDomain;
  responsiblePerson?: string;
  description?: string;
}
