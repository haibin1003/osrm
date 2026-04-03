export interface Category {
  id: number;
  categoryName: string;
  categoryCode: string;
  parentId: number | null;
  parentName: string | null;
  sortOrder: number;
  description: string | null;
  children: Category[];
}

export interface CategoryForm {
  categoryName: string;
  categoryCode: string;
  parentId: number | null;
  sortOrder?: number;
  description?: string;
}

export interface Tag {
  id: number;
  tagName: string;
  tagCode: string;
  description: string | null;
  createdAt: string;
}

export interface TagForm {
  tagName: string;
  tagCode: string;
  description?: string;
}
