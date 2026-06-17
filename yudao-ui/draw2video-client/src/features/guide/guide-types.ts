export interface GuideContent {
  id: number;
  title: string;
  slug: string;
  category: string;
  summary?: string;
  content?: string;
  coverUrl?: string;
  sort?: number;
  publishStatus?: string;
  publishTime?: string;
  updateTime?: string;
}

