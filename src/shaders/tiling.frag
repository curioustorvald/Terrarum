/*

*/


#ifdef GL_ES
precision mediump float;
#endif

layout(origin_upper_left,pixel_center_integer) in vec4 gl_FragCoord; // is now top-down and strictly integer

in vec4 v_color;
in vec2 v_texCoords;

uniform sampler2D u_texture;


uniform vec2 tilesInAxes; // 8x8

uniform sampler2D tilemap; // RGBA8888
uniform sampler2D tilemap2; // RGBA8888

uniform sampler2D tilesAtlas; // terrain, wire, fluids, etc.
uniform sampler2D tilesBlendAtlas; // alternative terrain for the weather mix (e.g. yellowed grass)
uniform float tilesBlend = 0.0; // percentage of blending [0f..1f]. 0: draws tilesAtlas, 1: draws tilesBlendAtlas

uniform vec2 tilesInAtlas = vec2(256.0, 256.0);
uniform vec2 atlasTexSize = vec2(2048.0, 2048.0);

vec2 _tilesInAtlas = vec2(1.0, 1.0) / tilesInAtlas;
vec2 tileSizeInPx = atlasTexSize * _tilesInAtlas; // should be like ivec2(8.0, 8.0)
vec2 _tileSizeInPx = vec2(1.0, 1.0) / tileSizeInPx; // should be like ivec2(0.125, 0.125)

uniform vec4 colourFilter = vec4(1, 1, 1, 1); // used by WALL to darken it

uniform ivec2 cameraTranslation = ivec2(0, 0); // used to offset the drawing; it's integer because we want the drawing to be pixel-aligned

uniform float drawBreakage = 1.0; // set it to 0f to not draw breakage, 1f to draw it; NEVER set to any other values.

uniform float mulBlendIntensity = 1.0; // used my MUL-blending drawings; works about the same way as the Layer Opacity slider of Photoshop/Krita/etc.

const vec2 bc = vec2(1.0, 0.0); //binary constant
const vec2 haalf = vec2(0.5, 0.5);

out vec4 fragColor;

ivec2 tileNumberToXY(int tileNumber) {
    return ivec2(tileNumber % int(tilesInAtlas.x), tileNumber / int(tilesInAtlas.x));
}

// return: ivec3(tileID, breakage, fliprot)
ivec3 _colToInt(vec4 map1, vec4 map2) {
    ivec3 col1 = ivec3(
        int(map1.r * 255),
        int(map1.g * 255),
        int(map1.b * 255));

    ivec3 col2 = ivec3(
        int(map2.r * 255),
        int(map2.g * 255),
        int(map2.b * 255));

    return ivec3(
        (col1.r << 16) | (col1.g << 8) | col1.b, // tile
        0,//col2.b, // breakage
        0//col2.g // fliprot
    );
}

mat3x2[] flipRotMat = mat3x2[](
mat3x2( 1.0,  0.0,  0.0,  1.0, tileSizeInPx.x*0.0, tileSizeInPx.y*0.0),
mat3x2(-1.0,  0.0,  0.0,  1.0, tileSizeInPx.x*1.0, tileSizeInPx.y*0.0),
mat3x2( 0.0, -1.0,  1.0,  0.0, tileSizeInPx.x*0.0, tileSizeInPx.y*1.0),
mat3x2( 0.0,  1.0,  1.0,  0.0, tileSizeInPx.x*0.0, tileSizeInPx.y*0.0),
mat3x2(-1.0,  0.0,  0.0, -1.0, tileSizeInPx.x*1.0, tileSizeInPx.y*1.0),
mat3x2( 1.0,  0.0,  0.0, -1.0, tileSizeInPx.x*0.0, tileSizeInPx.y*1.0),
mat3x2( 0.0,  1.0, -1.0,  0.0, tileSizeInPx.x*1.0, tileSizeInPx.y*0.0),
mat3x2( 0.0, -1.0, -1.0,  0.0, tileSizeInPx.x*1.0, tileSizeInPx.y*1.0)
);

vec2 uvFlipRot(int op, vec2 uv) {
    return (flipRotMat[op] * vec3(uv, 1.0)).xy;
}

void main() {

    // READ THE FUCKING MANUAL, YOU DONKEY !! //
    // This code purposedly uses flipped fragcoord. //
    // Make sure you don't use gl_FragCoord unknowingly! //
    // Remember, if there's a compile error, shader SILENTLY won't do anything //


    // default gl_FragCoord takes half-integer (represeting centre of the pixel) -- could be useful for phys solver?
    // This one, however, takes exact integer by rounding down. //
    vec2 overscannedScreenDimension = tilesInAxes * tileSizeInPx; // how many tiles will fit into a screen; one used by the tileFromMap; we need this because screen size is not integer multiple of the tile size
    vec2 fragCoord = gl_FragCoord.xy + cameraTranslation + haalf; // manually adding half-int to the flipped gl_FragCoord: this avoids driver bug present on the Asahi Linux and possibly (but unlikely) others

    // get required tile numbers //

    vec4 tileFromMap = texture(tilemap, fragCoord / overscannedScreenDimension); // raw tile number
    vec4 tileFromMap2 = texture(tilemap2, fragCoord / overscannedScreenDimension); // raw tile number
    ivec3 tbf = _colToInt(tileFromMap, tileFromMap2);
    int tile = tbf.x;
    int breakage = tbf.y;
    int flipRot = tbf.z;
    ivec2 tileXY = tileNumberToXY(tile);
    ivec2 breakageXY = tileNumberToXY(breakage + 5); // +5 is hard-coded constant that depends on the contents of the atlas

    // calculate the UV coord value for texture sampling //

    // don't really need highp here; read the GLES spec
    vec2 uvCoordForTile = uvFlipRot(flipRot, mod(fragCoord, tileSizeInPx)) * _tileSizeInPx * _tilesInAtlas; // 0..0.00390625 regardless of tile position in atlas
    vec2 uvCoordOffsetTile = tileXY * _tilesInAtlas; // where the tile starts in the atlas, using uv coord (0..1)
    vec2 uvCoordOffsetBreakage = breakageXY * _tilesInAtlas;

    // get final UV coord for the actual sampling //

    vec2 finalUVCoordForTile = uvCoordForTile + uvCoordOffsetTile;// where we should be actually looking for in atlas, using UV coord (0..1)
    vec2 finalUVCoordForBreakage = uvCoordForTile + uvCoordOffsetBreakage;

    // blending a breakage tex with main tex //

    vec4 tileCol = texture(tilesAtlas, finalUVCoordForTile);
    vec4 tileAltCol = texture(tilesBlendAtlas, finalUVCoordForTile);

    vec4 finalTile = mix(tileCol, tileAltCol, tilesBlend);

    vec4 finalBreakage = drawBreakage * texture(tilesAtlas, finalUVCoordForBreakage); // drawBreakeage = 0 to not draw, = 1 to draw

    vec4 finalColor = fma(mix(finalTile, finalBreakage, finalBreakage.a), bc.xxxy, finalTile * bc.yyyx);

//    fragColor = mix(colourFilter, colourFilter * finalColor, mulBlendIntensity);
    fragColor = finalTile;


    // SUBTILE fixme:
    // - breakage tile samples wrong coord -- needs bigtile-to-subtile adaptation
    // - somehow make fliprot work again -- needs bigtile-to-subtile adaptation

}
