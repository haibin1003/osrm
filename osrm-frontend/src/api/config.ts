import request from './request';

export const configApi = {
  getAll() {
    return request.get<Record<string, string>>('/v1/config');
  },
  update(configs: Record<string, string>) {
    return request.put('/v1/config', configs);
  }
};
