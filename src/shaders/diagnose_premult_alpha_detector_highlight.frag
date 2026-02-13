
in vec2 v_texCoords;
uniform sampler2D u_texture;
out vec4 fragColor;

// This version highlights pixels that will cause halos
// Bright red = guaranteed fringe artifact.

void main() {
    vec4 c = texture(u_texture, v_texCoords);
    float maxRGB = max(c.r, max(c.g, c.b));

    float diff = maxRGB - c.a;

    // amplify mismatch
    float halo = clamp(abs(diff) * 10.0, 0.0, 1.0);

    fragColor = vec4(halo, 0, 0, 1); // red glow where halos will appear
}

