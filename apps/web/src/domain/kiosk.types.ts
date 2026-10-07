export interface KioskStatus {
  kioskId: string;
  storeId: string;
  name: string;
  online: boolean;
  visionConnected: boolean;
  backendConnected: boolean;
  activeCampaignCount: number;
  lastHeartbeat: string;
}
