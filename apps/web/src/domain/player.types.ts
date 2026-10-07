export type PlaybackStatus = 'idle' | 'playing' | 'buffering' | 'error';

export interface PlaybackItem {
  id: string;
  campaignId: string;
  campaignName: string;
  mediaUrl: string;
  mediaType: 'image' | 'video';
  durationSeconds: number;
}

export interface PlayerState {
  status: PlaybackStatus;
  currentPlayback?: PlaybackItem;
  kioskId: string;
  updatedAt: string;
}
