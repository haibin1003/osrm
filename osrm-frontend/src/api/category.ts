import request from './request';
import type { Category, CategoryForm, Tag, TagForm } from '@/types/category';

export const categoryApi = {
  getTree() {
    return request.get<Category[]>('/v1/categories');
  },
  getById(id: number) {
    return request.get<Category>(`/v1/categories/${id}`);
  },
  create(data: CategoryForm) {
    return request.post<Category>('/v1/categories', data);
  },
  update(id: number, data: CategoryForm) {
    return request.put<Category>(`/v1/categories/${id}`, data);
  },
  delete(id: number) {
    return request.delete(`/v1/categories/${id}`);
  }
};

export const tagApi = {
  findAll() {
    return request.get<Tag[]>('/v1/tags');
  },
  create(data: TagForm) {
    return request.post<Tag>('/v1/tags', data);
  },
  delete(id: number) {
    return request.delete(`/v1/tags/${id}`);
  }
};
