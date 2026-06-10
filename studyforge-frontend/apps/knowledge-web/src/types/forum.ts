export interface ForumThreadNode {
  id: number;
  parentId?: number | null;
  userId: number;
  authorUsername: string;
  authorName: string;
  authorAvatarUrl: string;
  parentAuthorName: string;
  content: string;
  floorNo: number;
  likeCount: number;
  likedByViewer: boolean;
  canDelete: boolean;
  deleted: boolean;
  accepted?: boolean;
  createdLabel: string;
  replies: ForumThreadNode[];
}
