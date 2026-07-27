# Design System Strategy: The Kinetic Fortress

### 1. Overview & Creative North Star
The Creative North Star for this design system is **"The Kinetic Fortress."** 

In the world of cybersecurity, "static" means "vulnerable." This system moves away from the flat, boxed-in nature of standard utility apps toward an editorial, high-tech experience that feels alive. We achieve this through **Layered Intelligence**: using depth, motion-ready gradients, and glassmorphism to suggest a UI that is constantly scanning, protecting, and evolving. 

By breaking the traditional grid with intentional asymmetry—such as overlapping glass cards and extreme typographic contrast—we signal to the user that this is not just an app, but a premium security instrument.

---

### 2. Colors & Surface Architecture
The palette is rooted in deep space navys and electric luminescent tones. However, the sophistication lies in how these colors are layered, not just applied.

*   **Primary (`#a2c9ff`) & Secondary (`#54d8e8`):** Use these for "Active Intelligence." The primary blue represents the core shield, while the cyan secondary is used for data-driven accents and secondary actions.
*   **The "No-Line" Rule:** Under no circumstances should 1px solid borders be used to define sections. Layout boundaries must be established through color shifts. For example, a card should sit as a `surface-container-low` block against a `surface` background. The eye should perceive the edge via the shift in depth, not a drawn line.
*   **Surface Hierarchy & Nesting:** Treat the UI as a series of physical glass sheets. 
    *   **Base:** `surface` (#0c1321)
    *   **Secondary Sections:** `surface-container-low` (#151b2a)
    *   **Interactive Cards:** `surface-container-high` (#232a39)
    *   **Floating Modals:** `surface-bright` (#323949)
*   **The "Glass & Gradient" Rule:** To achieve a signature "Cybersecurity" feel, use a 45-degree linear gradient for primary CTAs, transitioning from `primary-container` (#4da3ff) to `primary` (#a2c9ff). For floating elements, use a backdrop-blur of 20px combined with a `surface-variant` fill at 40% opacity.

---

### 3. Typography
Our typography creates an editorial hierarchy that feels authoritative yet futuristic.

*   **The Display Scale (Space Grotesk):** Use `display-lg` and `headline-lg` for high-impact data points (e.g., "98% Protected"). The geometric nature of Space Grotesk evokes a technical, digital-first aesthetic.
*   **The Narrative Scale (Manrope):** Use `title-lg` and `body-lg` for all instructional content. Manrope’s balanced proportions ensure that even complex security jargon remains readable.
*   **The Functional Scale (Inter):** Use `label-md` for micro-copy and metadata. Inter provides the highest legibility at small scales, perfect for timestamps or technical logs.
*   **Intentional Contrast:** Pair a `display-sm` headline with a `label-md` sub-header in `on-surface-variant` to create a high-end, asymmetric lockup that feels custom-designed.

---

### 4. Elevation & Depth
Depth is our primary tool for communicating security. A "flat" app feels flimsy; a "layered" app feels fortified.

*   **The Layering Principle:** Avoid shadows for static elements. Instead, nest `surface-container-highest` inside `surface-container-lowest`. This "recessed" look creates a sophisticated, tactile feel without the clutter of drop shadows.
*   **Ambient Shadows:** When a component must float (like a bottom sheet or a critical alert), use a shadow with a 32px blur, 0px offset, and 8% opacity. The shadow color must be tinted with the `surface-tint` (#a2c9ff) to ensure it looks like light passing through blue glass, rather than a muddy grey.
*   **The "Ghost Border" Fallback:** If accessibility requires a stroke (e.g., in high-glare environments), use the `outline-variant` token at **15% opacity**. It should be felt, not seen.
*   **Glassmorphism:** Use Backdrop Filter: Blur (20px) on all floating overlays. This ensures the "Matrix" background persists through the UI, maintaining the brand’s depth even in deep navigation.

---

### 5. Components

*   **Buttons:**
    *   **Primary:** A gradient fill (`primary-container` to `primary`) with `on-primary-container` text. Use `xl` (0.75rem) roundedness.
    *   **Secondary:** No fill. A "Ghost Border" (15% `outline-variant`) with `primary` text.
*   **Input Fields:** Use `surface-container-highest` for the field background. No bottom line. Use `md` (0.375rem) roundedness. The cursor and active label should always be `secondary` (#54d8e8).
*   **Cards & Lists:** **Strictly prohibit divider lines.** Separate list items using `8px` of vertical whitespace or a subtle background shift to `surface-container-low` on hover/press.
*   **Chips:** Use `surface-variant` for inactive chips. When active, use a `secondary` glow effect (a soft 4px outer blur in the accent color).
*   **Security Gauges (Custom):** For data visualization, use thick, rounded strokes with `tertiary` (#45dfa4) for success and `error` (#ffb4ab) for threats. The "unfilled" part of the gauge should be `surface-container-highest`.
*   **Checkboxes & Radios:** Use the `secondary` cyan for the "Selected" state. These should feel like "active nodes" in a network.

---

### 6. Do’s and Don’ts

**Do:**
*   Use asymmetric layouts (e.g., left-aligned headlines with right-aligned data visualization).
*   Use `surface-container` tiers to create a "nested" look.
*   Apply `backdrop-blur` to all overlays to maintain the "Cybersecurity" depth.
*   Use `primary-fixed-dim` for text on dark backgrounds to ensure AAA accessibility without losing the blue hue.

**Don’t:**
*   **Don’t** use pure black (#000000) or pure white (#FFFFFF). Use our specific surface and "on-surface" tokens.
*   **Don’t** use 1px solid lines for decoration or sectioning.
*   **Don’t** use standard Material 3 "rounded-full" buttons for everything; stick to `xl` (12px) for a more modern, architectural feel.
*   **Don’t** use generic grey shadows. Always tint shadows with the primary blue palette.