## 2023-11-20 - Adding Tooltips to Icon-Only Buttons
**Learning:** In the `ChatScreen` UI, the icon-only buttons (cancel and send) were missing tooltips, making their purpose ambiguous to users relying on screen readers or needing more context.
**Action:** Always add tooltips to icon-only buttons. Furthermore, ensure that the tooltips use the project's custom i18n system (`com.lewho.i18n.CCText`) instead of hardcoded strings to support localization.
