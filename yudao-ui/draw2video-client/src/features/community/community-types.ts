export interface PageResult<T> {
  list: T[];
  total: number;
}

export interface CommunityPost {
  id: number;
  postNo?: string;
  authorUserId: number;
  authorNickname?: string;
  authorAvatarUrl?: string;
  assetId?: number;
  projectId?: number;
  coverAssetId?: number;
  assetType?: string;
  coverUrl?: string;
  fileUrl?: string;
  title: string;
  summary?: string;
  tags?: string;
  promptSnapshot?: string;
  metadata?: string;
  visibility?: string;
  publishStatus?: string;
  auditStatus?: string;
  auditReason?: string;
  publishTime?: string;
  viewCount?: number;
  likeCount?: number;
  commentCount?: number;
  shareCount?: number;
  downloadCount?: number;
  hotScore?: number;
  likedByCurrentUser?: boolean;
  followedAuthor?: boolean;
  createTime?: string;
}

export interface CommunityComment {
  id: number;
  postId: number;
  userId: number;
  userNickname?: string;
  userAvatarUrl?: string;
  parentId?: number;
  content: string;
  auditStatus?: string;
  auditReason?: string;
  status?: string;
  likeCount?: number;
  mine?: boolean;
  createTime?: string;
}

export interface CommunityAuthor {
  authorUserId: number;
  nickname?: string;
  avatarUrl?: string;
  followerCount?: number;
  followingCount?: number;
  publicPostCount?: number;
  likeReceivedCount?: number;
  followedByCurrentUser?: boolean;
}

export interface CommunityShare {
  shareUrl: string;
  shareToken: string;
  shareCount: number;
}
