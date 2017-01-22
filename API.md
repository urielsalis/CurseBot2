### Full Curse API Reference

**Authenticate with Curse**

https://logins-v1.curseapp.net/login

Request: *POST*

Requires: `Username`, `Password`

Returns an authentication token

All requests below require that you have a valid authentication token specified in the headers of the request

`headers = { 'AuthenticationToken' : 'AUTHENTICATION_TOKEN_RECIEVED_FROM_LOGIN' }`

*If no "returns" is specified, look for a 200 response code instead.*

**Post a new message**

https://conversations-v1.curseapp.net/conversations/<channelid>

Request: *POST*

Requires `ClientID`, `MachineKey`, `Body`

Also send:

`AttachmentID = 00000000-0000-0000-0000-000000000000`

`AttachmentRegionID = 0`

**Get information from group**

https://groups-v1.curseapp.net/groups/<groupid>?showDeletedChannels=false

Request: *GET*

Returns JSON with information about group, see [sample reference](http://pastebin.com/jwBbuawZ)

**Get last messages in a group**

https://conversations-v1.curseapp.net/conversations/<channelid>?endTimestamp=0&pageSize=30&startTimestamp=0

Request: *GET*

Returns JSON with last 30 messages, see [sample reference](http://pastebin.com/YqV4Dy3D)

**Delete a message**

https://conversations-v1.curseapp.net/conversations/<channelid>/<serverid>-<timestamp>

Request: *DELETE*

**Kick a member**

https://groups-v1.curseapp.net/groups/<groupid>/members/<memberid>

Request: *DELETE*

**"Like" a message**

https://conversations-v1.curseapp.net/conversations/<channelid>/<serverId>-<timestamp>/like  GG

Request: *POST*

**Edit a message**

https://conversations-v1.curseapp.net/conversations/<channelID>/<serverId>-<timestamp>

Request: *POST*

Requires: `Body`, optionally `Mentions`

**Get active/inactive members in a group**

https://groups-v1.curseapp.net/groups/<channelid>/members?actives=true&page=1&pageSize=50

Note: Change `actives` and `pageSize` for varying results.

Request: *GET*

Returns: Active or inactive members of the group, with the limit of pageSize. [Sample](http://pastebin.com/Sn9iMFN1)

**Get information about a member**

https://groups-v1.curseapp.net/groups/<groupID>/members/<memberid>

Request: *GET*

Returns: Information about the user

**Change nickname of user**

https://groups-v1.curseapp.net/groups/<groupID>/nickname

Request: *POST*

Requires: `Nickname`

**Create a Curse server**

https://groups-v1.curseapp.net/servers

Request: *POST*

Requires: `Title`, `TextChannelTitle`, `VoiceChannelTitle`, `OwnerRoleName`, `GuestRoleName`, `ModeratorRoleName`, `IsPublic`

**Leave a Curse server**

https://groups-v1.curseapp.net/groups/ `<groupID>` /leave

Request: *POST*

**Ban a user**

https://groups-na-v1.curseapp.net/servers/ `<group-id>` /bans

Request: *POST*

Requires: `UserID`, `Reason`, `MessageDeleteMode` (default==0)

**Unban a user**

https://groups-na-v1.curseapp.net/servers/ `<group-id>` /bans/ `<user-id>`

Request: *DELETE*

**Curse search**

https://groups-v1.curseapp.net/servers/search

Note: All contents of the payload can be null

Request: *POST*

Requires: `Query`, `PageNumber`, `PageSize`, `GroupTitle`, `OwnerUsername, `MinMemberCount`, `MaxMemberCount`, `Tags`, `Games`, `IsPublic`, `SortType`, `SortAscending`, `IncludeInappropriateContent`