
in vec2 v_texCoords;
uniform sampler2D u_texture;
out vec4 fragColor;

void main() {
    vec4 c = texture(u_texture, v_texCoords);

    float maxRGB = max(c.r, max(c.g, c.b));

    // Visualise mismatches:
    // RED = looks like straight alpha
    // BLUE = looks like premultiplied alpha
    // GREEN = impossible / corrupted

    if (maxRGB > c.a + 0.001) {
        // RGB > A → straight alpha
        fragColor = vec4(1, 0, 0, 1);
    }
    else if (maxRGB < c.a - 0.001) {
        // RGB < A → premultiplied alpha
        fragColor = vec4(0, 0, 1, 1);
    }
    else {
        // borderline / linear content
        fragColor = vec4(0, 1, 0, 1);
    }
}
