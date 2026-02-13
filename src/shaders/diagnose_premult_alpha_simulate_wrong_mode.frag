
in vec2 v_texCoords;
uniform sampler2D u_texture;
out vec4 fragColor;

// This shows how the sprite *would* look if alpha mode were wrong
// If you see garbage edges, your texture is premultiplied.

void main() {
    vec4 c = texture(u_texture, v_texCoords);

    // simulate wrong blending
    vec3 wrong = c.rgb / max(c.a, 0.001);

    fragColor = vec4(wrong, 1.0);
}


