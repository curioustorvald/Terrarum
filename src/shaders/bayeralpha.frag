#ifdef GL_ES
precision mediump float;
#endif

in vec4 v_color;
in vec2 v_texCoords;

uniform sampler2D u_texture; // world texture, has alpha value that is meaningful
uniform float acount = 2.0;
uniform int frame = 0;

out vec4 fragColor;

const int bayer[4 * 4] = int[](0,8,2,10,12,4,14,6,3,11,1,9,15,7,13,5);
const vec2 bayerOffset[4 * 4] = vec2[](
    vec2(0,0),
    vec2(3,2),
    vec2(1,3),
    vec2(3,0),
    vec2(1,1),
    vec2(2,3),
    vec2(0,2),
    vec2(2,1),
    vec2(3,3),
    vec2(1,0),
    vec2(3,1),
    vec2(0,3),
    vec2(2,2),
    vec2(0,1),
    vec2(2,0),
    vec2(1,2)
);
const float bayerSize = 4.0;
const float bayerDivider = bayerSize * bayerSize;

float nearestAlpha(float alpha) {
    return min(1.0, floor(alpha + 0.5));
}

void main() {
    vec4 inColor = texture(u_texture, v_texCoords) * v_color;
    vec2 entryOffset = bayerOffset[int(mod(frame, bayerDivider))];
    vec2 entry = mod(gl_FragCoord.xy + entryOffset, vec2(bayerSize, bayerSize));


//    float alpha = nearestAlpha((inColor.a * 16.0 / 15.0) + (bayer[int(entry.y) * int(bayerSize) + int(entry.x)] / bayerDivider - 0.5));
    float alpha = nearestAlpha((inColor.a * 16.0 / 15.0) + (bayer[int(entry.y) * int(bayerSize) + int(entry.x)] / bayerDivider - 0.5));

    fragColor = vec4(inColor.rgb, alpha);
}
