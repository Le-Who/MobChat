# Project world-space bubbles into client input and rendering

Bubble rendering uses nearby entities and refreshes that set every third render call. Player messages render regardless of the mob filter. The blacklist wins over the whitelist. Only mob bubbles register click targets because the player bubbles carry no mob interaction. Use the Ender Dragon head for the bubble anchor and place ordinary mob bubbles slightly forward from the entity based on its width and yaw.

The texture code uses the entity renderer's texture to locate the corresponding MobChat icon. Player icons use the skin's black and white marker pixels and the unused UV tiles. Render both face and hat layers when falling back to the normal skin portrait. Use a small world scale for text and icons, depth-test them against the world, and keep transparent overlays from writing depth where the adapter supports that operation. The orientation of LEAD arrows follows Minecraft yaw, including its half-turn correction, and the arrow particle carries the angle separately from speed.

Start click rays at the camera for first- and third-person views. A billboard anchored at its bottom edge needs an upward half-height offset before deriving the corners. For a vertical view, the normal cross-product axes can become degenerate, so retain a fallback right vector. The previous-page region is narrower than the forward region because the forward path is the primary action.

Client packet callbacks check the originating connection after dispatch to the client thread. Disconnect clears partially assembled sync chunks, chat projections, filters, and typing state. A setup draft belongs to its screen while a language picker covers it. The server owns authorization and stored keys. The locale selector uses an empty code for automatic language selection and displays locale names in their own scripts.

Chat wrapping approximates visual width and breaks long segments even when there is no whitespace, as in some CJK text. The HTML test comparison escapes dynamic strings before adding its own display markup.
