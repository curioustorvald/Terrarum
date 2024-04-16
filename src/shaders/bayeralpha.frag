#ifdef GL_ES
precision mediump float;
#endif

in vec4 v_color;
in vec2 v_texCoords;

uniform sampler2D u_texture; // world texture, has alpha value that is meaningful
uniform float acount = 2.0;

out vec4 fragColor;

const int bayer[4 * 4] = int[](0,8,2,10,12,4,14,6,3,11,1,9,15,7,13,5);
const float bayerSize = 4.0;
const float bayerDivider = bayerSize * bayerSize;

float nearestAlpha(float alpha) {
    return min(1.0, floor(alpha + 0.5));
}

void main() {
    vec4 inColor = texture(u_texture, v_texCoords) * v_color;
    vec2 entry = mod(gl_FragCoord.xy, vec2(bayerSize, bayerSize));

    float alpha = nearestAlpha((inColor.a * 16.0 / 15.0) + (bayer[int(entry.y) * int(bayerSize) + int(entry.x)] / bayerDivider - 0.5));

    fragColor = vec4(inColor.rgb, alpha);
}
