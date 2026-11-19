export type NoteType = 'GENERAL' | 'JOURNAL';

export type NoteSortField =
  'createdAt' | 'updatedAt' | 'title' | 'noteType' | 'pinned' | 'journalDate';

export interface NoteRequest {
  title: string;
  content: string;
  noteType: NoteType;
  journalDate: string | null;
}

export interface NoteResponse extends NoteRequest {
  id: number;
  pinned: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface NotePageResponse {
  content: NoteResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface NoteQuery {
  page?: number;
  size?: number;
  search?: string;
  sortBy?: NoteSortField;
  direction?: 'ASC' | 'DESC';
}

export interface NoteStatisticsResponse {
  totalNotes: number;
  generalNotes: number;
  journalEntries: number;
  pinnedNotes: number;
}
