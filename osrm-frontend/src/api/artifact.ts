import request from './request';

export interface ArtifactUploadResult {
  success: boolean;
  filePath: string;
  fileSize: number;
  md5Hash: string;
  downloadCommand: string;
  message: string;
}

export const artifactApi = {
  upload(packageId: number, versionId: number, file: File) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('packageId', String(packageId));
    formData.append('versionId', String(versionId));
    return request.post<ArtifactUploadResult>('/v1/artifacts/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 300000 // 5分钟超时
    });
  }
};
