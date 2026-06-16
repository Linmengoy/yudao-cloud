export interface ReleaseNote {
  id: number;
  version: string;
  releaseDate: string;
  title: string;
  summary?: string | null;
  content?: string | null;
  publishTime?: string | null;
}
