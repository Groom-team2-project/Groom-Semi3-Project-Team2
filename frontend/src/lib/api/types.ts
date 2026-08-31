/**
 * 도메인 타입 정의.
 * 최종 ERD(users / plans / members / invitations / schedules / places /
 * votes / vote_options / vote_participations / comments / activity_logs) 기준.
 * 지금은 mock 데이터를 이 타입으로 리턴하고, 백엔드 연동 시 API 응답을
 * 이 타입에 맞춰 매핑하면 됩니다.
 */

export type Role = "OWNER" | "EDITOR" | "VIEWER";

export interface User {
  id: string;
  name: string;
  email: string;
  profileImage?: string;
  avatarColor: string;
  avatarInitial: string;
}

export interface Member {
  id: string;
  userId: string;
  name: string;
  avatarColor: string;
  avatarInitial: string;
  role: Role;
  status?: "JOINED" | "LEFT";
}

export interface Plan {
  id: string;
  title: string;
  description?: string;
  emoji?: string;
  startDate: string; // ISO date (YYYY-MM-DD)
  endDate: string; // ISO date (YYYY-MM-DD)
  capacity?: number;
  ownerId: string;
  members: Member[];
  createdAt: string;
  myRole?: Role;
  memberCount?: number;
}

export interface Invitation {
  code: string;
  url: string;
  expiresAt: string;
  status?: "ACTIVE" | "EXPIRED" | "REVOKED";
  invitationId?: string;
  planId?: number;
}

export type PlaceUsage = "schedule" | "vote_candidate" | "saved";
export type PlaceSource = "KAKAO_LOCAL";

export interface Place {
  id: string;
  planId: string;
  name: string;
  address: string;
  emoji: string;
  category?: string;
  source: PlaceSource;
  usage: PlaceUsage[];
}

/** 카카오 로컬 API 검색 결과 (아직 저장 전) */
export interface PlaceSearchResult {
  kakaoId: string;
  selectionToken?: string;
  name: string;
  address: string;
  landLotAddress?: string;
  emoji: string;
  category?: string;
  categoryGroupCode?: string;
  categoryGroupName?: string;
  roadAddress?: string;
  longitude?: number;
  latitude?: number;
  phone?: string;
  placeUrl?: string;
  distanceMeters?: number;
}

export interface Schedule {
  id: string;
  planId: string;
  placeId?: string;
  day: number; // Day 1, 2, 3 ...
  date: string; // ISO date
  time: string; // "09:00"
  placeName: string;
  placeAddress?: string;
  emoji: string;
  memo?: string;
  kakaoRouteUrl?: string;
  linkedVoteId?: string; // 투표중인 자리인 경우
}

export interface Comment {
  id: string;
  planId?: string;
  scheduleId: string;
  parentCommentId?: string;
  userId?: string;
  authorName: string;
  authorColor: string;
  profileImage?: string;
  text: string;
  deleted?: boolean;
  createdAt: string;
  likeCount: number;
  likedByMe: boolean;
}

export type VoteStatus = "OPEN" | "CLOSED";

export interface VoteOption {
  id: string;
  voteId: string;
  placeName: string;
  placeAddress?: string;
  emoji: string;
  voteCount: number;
}

export interface Vote {
  creatorId: string;
  id: string;
  planId: string;
  title: string;
  status: VoteStatus;
  deadline: string; // ISO datetime
  options: VoteOption[];
  myOptionId?: string;
  linkedScheduleId?: string;
  resultSummary?: string; // 마감된 투표의 확정 결과 요약
}

export type ActivityType =
  | "schedule_added"
  | "schedule_updated"
  | "schedule_deleted"
  | "vote_created"
  | "vote_updated"
  | "vote_deleted"
  | "vote_participated"
  | "vote_closed"
  | "member_joined"
  | "member_left"
  | "member_role_changed"
  | "invitation_created"
  | "invitation_revoked"
  | "invitation_reissued"
  | "comment_added"
  | "comment_deleted"
  | "comment_liked";

export interface ActivityLog {
  id: string;
  planId: string;
  planTitle?: string;
  actorName: string;
  actorColor: string;
  actorInitial: string;
  type: ActivityType;
  summary: string;
  targetType?: "schedule" | "vote" | "member" | "comment" | "invitation";
  targetId?: string;
  scheduleId?: string;
  createdAt: string; // ISO datetime
}
