

The following file contains the current feature list for Arcturus Morningstar as of the 4.x Beta Branch. 
We hope this file will provide an easy place to find functions in Arcturus Morningstar for new developers, as well as give people the chance to see exactly what Arcturus Morningstar can do!

If you wish to contribute to this list, features are laid out in the following format:



## ✍️ Example Header:

##### Example Feature Header- ✔️ (completed) ⭕ (incomplete/ not implemented)

> [`ExampleLinkToRelatedClasses`](https://google.com) 
>
> ###### Example Sub Feature Header - ✔️
>
> > [`ExampleLinkToRelatedSubClasses`](https://google.com) 





## 🖥️  Connection / User:

##### Login via SSO Ticket ✔️

> [`SecureLoginEvent`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/messages/incoming/handshake/SecureLoginEvent.java)  
> [`HabboManager.loadHabbo()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/HabboManager.java#L104)

##### Support RSA Encryption ✔️

> [`HabboRSACrypto`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/crypto/HabboRSACrypto.java)  
> [`HabboRC4`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/crypto/HabboRC4.java)  
> [`HabboDiffieHellman`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/crypto/HabboDiffieHellman.java)  
> [`CompleteDiffieHandshakeEvent`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/messages/incoming/handshake/CompleteDiffieHandshakeEvent.java)  
> [`InitDiffieHandshakeEvent`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/messages/incoming/handshake/InitDiffieHandshakeEvent.java)  



## 🧸 RCON:

##### RCON ✔️ 

> [`RCONMessage`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/master/src/main/java/com/eu/habbo/messages/rcon/RCONMessage.java) 
>
> ###### RCON Messages - ✔️
>
> > [`AlertUser`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/master/src/main/java/com/eu/habbo/messages/rcon/AlertUser.java))  
> > [`ChangeRoomOwner`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/master/src/main/java/com/eu/habbo/messages/rcon/ChangeRoomOwner.java)  
> > [`CreateModToolTicket`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/master/src/main/java/com/eu/habbo/messages/rcon/CreateModToolTicket.java)  
> > [`DisconnectUser`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/master/src/main/java/com/eu/habbo/messages/rcon/DisconnectUser.java)  
> > [`ExecuteCommand`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/master/src/main/java/com/eu/habbo/messages/rcon/ExecuteCommand.java)  
> > [`ForwardUser`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/master/src/main/java/com/eu/habbo/messages/rcon/ForwardUser.java)  
> > // todo finish this

##### 

## 💠 Subscriptions:

###### Subscriptions Manager  ✔️

> > [`Subscription`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/subscriptions/Subscription.java)  
> > [`SubscriptionManager`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/subscriptions/SubscriptionManager.java)  
> > [`SubscriptionScheduler`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/subscriptions/SubscriptionScheduler.java)  
> > [`UserSubscriptionCreatedEvent`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/plugin/events/users/subscriptions/UserSubscriptionCreatedEvent.java)  
> > [`UserSubscriptionExpiredEvent`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/plugin/events/users/subscriptions/UserSubscriptionExpiredEvent.java)  
> >  [`UserSubscriptionExtendedEvent`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/master/src/main/java/com/eu/habbo/messages/rcon/RCONMessage.java) 
> >
> > ##### Habbo Club - ✔️ 
> >
> > > [`SubscriptionHabboClub`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/subscriptions/SubscriptionHabboClub.java)  
> > > [`RequestUserClubEvent`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/messages/incoming/users/RequestUserClubEvent.java)  
> > > [`RequestClubDataEvent`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/messages/incoming/catalog/RequestClubDataEvent.java)  
> > > [`ClubDataComposer`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/messages/outgoing/catalog/ClubDataComposer.java)  
> > > [`HabboStats.hasActiveClub()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/HabboStats.java#L555)
> > >
> > > ###### HC Catalogue - ✔️
> > >
> > > > [`ClubBuyLayout`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/catalog/layouts/ClubBuyLayout.java)  
> > > > [`ClubOffer`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/catalog/ClubOffer.java)  
> > > > [`ClubGiftsLayout`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/catalog/layouts/ClubGiftsLayout.java)  
> > > > [`ClubGiftsComposer`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/messages/outgoing/catalog/ClubGiftsComposer.java)  
> > > > [`ClubCenterDataComposer`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/messages/outgoing/catalog/ClubCenterDataComposer.java)  
> > > > [`ClubGiftReceivedComposer`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/messages/outgoing/users/ClubGiftReceivedComposer.java)  
> > >
> > > ###### HC Payday - ✔️
> > >
> > > > [`SecureLoginEvent`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/messages/incoming/handshake/SecureLoginEvent.java#L202)  
> > > > [`SubscriptionScheduler`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/subscriptions/SubscriptionScheduler.java)  
> > > > [`SubscriptionHabboClub.calculatePayDay()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/subscriptions/SubscriptionHabboClub.java#L184)  
> > > > [`SubscriptionHabboClub.executePayDay()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/subscriptions/SubscriptionHabboClub.java#L257)  
> > > > [`SubscriptionHabboClub.processUnclaimed()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/subscriptions/SubscriptionHabboClub.java#L316)  
> > > > [`SubscriptionHabboClub.claimPayDay()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/subscriptions/SubscriptionHabboClub.java#L368)  
> > > > [`SubscriptionHabboClub.progressAchievement()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/subscriptions/SubscriptionHabboClub.java#L419)
> > >
> > > ###### HC Checks on clothing - ✔️
> > >
> > > > [`ClothingValidationManager`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/clothingvalidation/ClothingValidationManager.java)  
> > > > [`ClothingValidationManager.validateLook()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/clothingvalidation/ClothingValidationManager.java#L61)  
> > >
> > > ###### HC dances - ✔️
> > >  
> > > > [`RoomUserDanceEvent`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/messages/incoming/rooms/users/RoomUserDanceEvent.java)  
> > > > [`RoomUserDanceComposer`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/messages/outgoing/rooms/users/RoomUserDanceComposer.java)  
> > > > [`RoomUnit.getDanceType()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/rooms/RoomUnit.java#L456)  
> > > > [`RoomUnit.setDanceType()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/rooms/RoomUnit.java#L460)  
> >
> > Builders Club - ⭕
> >
> > > [`SubscriptionScheduler`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/subscriptions/SubscriptionScheduler.java)  
> > > [`BuildersClubExpiredComposer`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/messages/outgoing/unknown/BuildersClubExpiredComposer.java)
> > >
> > > ###### Builders Club Catalogue - ⭕
> > >
> > > > [`BuildersClubAddonsLayout`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/catalog/layouts/BuildersClubAddonsLayout.java)  
> > > > [`BuildersClubLoyaltyLayout`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/catalog/layouts/BuildersClubLoyaltyLayout.java))  
> > > > [`BuildersClubFrontPageLayout`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/catalog/layouts/BuildersClubFrontPageLayout.java)  
> >
> > 



## 🤹 Entities:

##### Habbo   ✔️

> [`Habbo`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/Habbo.java)  
> [`Habbo.getClient()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/Habbo.java#L110)  
> [`Habbo.isOnline()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/Habbo.java#L64)  
> [`Habbo.getHabboInfo()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/Habbo.java#L90)  
> [`Habbo.getHabboStats()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/Habbo.java#L94)  
> [`Habbo.getRoomUnit()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/Habbo.java#L102)  
> [`HabboManager`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/HabboManager.java)  
> [`HabboManager.getOfflineHabboInfo()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/HabboManager.java#L47)  
> [`HabboManager.getCloneAccounts()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/HabboManager.java#L203)  
> [`HabboManager.setRank()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/HabboManager.java#L243)   
> [`HabboInfo`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/HabboManager.java)  

> ###### Clothing - ✔️
>
>
> > [`UserClothesComposer`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/messages/outgoing/users/UserClothesComposer.java)   
> > [`HabboInventory.getWardrobeComponent()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/HabboInventory.java#L67)   
> > [`HabboInventory.setWardrobeComponent()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/HabboInventory.java#L71)   
>
> ###### Inventory - ✔️
>
>
> > [`HabboInventory`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/HabboInventory.java)   
> > [`Habbo.getInventory()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/Habbo.java#L98)   
> > [`ItemsComponent.addItem()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/inventory/ItemsComponent.java#L67)   
> > [`ItemsComponent.addItems()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/inventory/ItemsComponent.java#L82)   
> > [`ItemsComponent.getHabboItem()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/inventory/ItemsComponent.java#L99)   
> > [`ItemsComponent.getAndRemoveHabboItem()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/inventory/ItemsComponent.java#L103)   
> > [`ItemsComponent.removeHabboItem()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/inventory/ItemsComponent.java#L126)   
> > [`ItemsComponent.getItemsAsValueCollection()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/inventory/ItemsComponent.java#L141)   
> > [`InventoryItemsComposer`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/HabboInfo.java#L265)   
> > [`InventoryBotsComposer`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/messages/outgoing/inventory/InventoryBotsComposer.java)   
> > [`InventoryPetsComposer`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/messages/outgoing/inventory/InventoryPetsComposer.java)   
> > [`InventoryAchievementsComposer`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/messages/outgoing/inventory/InventoryAchievementsComposer.java)   
> > [`InventoryRefreshComposer`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/messages/outgoing/inventory/InventoryRefreshComposer.java)   
> > [`InventoryItemsAddedEvent`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/plugin/events/inventory/InventoryItemsAddedEvent.java)   
> > [`InventoryItemEvent`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/plugin/events/inventory/InventoryItemEvent.java)   
>
>  ###### Motto - ✔️
>
>
> > [`HabboInfo.setMotto()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/HabboInfo.java#L269)   
> > [`HabboInfo.getMotto()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/HabboInfo.java#L265)   
>
> ###### Badges - ✔️
>
>
> > [`BadgesComponent`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/inventory/BadgesComponent.java)   
> > [`BadgesComponent.loadBadges()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/inventory/BadgesComponent.java#L28)   
> > [`BadgesComponent.getBadgesOfflineHabbo()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/inventory/BadgesComponent.java#L75)   
> > [`BadgesComponent.createBadge()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/inventory/BadgesComponent.java#L90)   
> > [`BadgesComponent.deleteBadge()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/inventory/BadgesComponent.java#L113)   
> > [`BadgesComponent.getWearingBadges()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/inventory/BadgesComponent.java#L123)   
> > [`BadgesComponent.hasBadge()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/inventory/BadgesComponent.java#L147)   
> > [`BadgesComponent.getBadge()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/inventory/BadgesComponent.java#L151)   
> > [`BadgesComponent.removeBadge()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/inventory/BadgesComponent.java#L167)   
>
> ##### Load Currency and Seasonal Currency - ✔️
>
> > [`RequestUserCreditsEvent`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/messages/incoming/users/RequestUserCreditsEvent.java)   
> > [`UserCurrencyComposer`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/messages/outgoing/users/UserCurrencyComposer.java)    
> > [`UserCreditsComposer`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/messages/outgoing/users/UserCreditsComposer.java)    
> > [`Habbo.getHabboInfo()`](https://git.krews.org/morningstar/Arcturus-Community/-/blob/dev/src/main/java/com/eu/habbo/habbohotel/users/Habbo.java#L90)    

> Save/Load Achievements
>
> Save/Load Friends
>
> Save/Load Own Rooms
>
> Save/Load Guilds
>
> Save/Load Currencies
>
> Save/Load Inventory
>
> Save/Load Friendships - Love/Hate/Like

​    

-
