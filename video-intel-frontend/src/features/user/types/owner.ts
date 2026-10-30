export type OwnerType = 'USER' | 'ANONYMOUS_SESSION';

export interface Owner {
  ownerType: OwnerType;
  ownerId: string;
}
