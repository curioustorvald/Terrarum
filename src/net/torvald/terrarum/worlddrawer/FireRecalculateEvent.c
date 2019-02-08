#include <xmmintrin.h>
#include <jni.h>
#include <stddef.h>
#include "net_torvald_terrarum_worlddrawer_LightmapRenderer.h"

/*
compile required headers with:
"C:\Program Files\Java\jdk1.8.0_131\bin\javah.exe" -jni -cp "./lib/gdx.jar;C:\Users\minjaesong\.IdeaIC2018.3\config\plugins\JBSDKDownloadHelper\lib\kotlin-runtime.jar;C:\Users\minjaesong\Documents\terrarum\build\libs\Terrarum-0.2.jar" net.torvald.terrarum.worlddrawer.LightmapRenderer

get method signatures with:
"C:\Program Files\Java\jdk1.8.0_131\bin\javap.exe" -s -p -cp "./lib/gdx.jar;C:\Users\minjaesong\.IdeaIC2018.3\config\plugins\JBSDKDownloadHelper\lib\kotlin-runtime.jar;C:\Users\minjaesong\Documents\terrarum\build\libs\Terrarum-0.2.jar" net.torvald.terrarum.gameworld.GameWorld
*/

#define vec4 __m128
#define overscan_open net_torvald_terrarum_worlddrawer_LightmapRenderer_overscan_open
#define DIV_FLOAT net_torvald_terrarum_worlddrawer_LightmapRenderer_DIV_FLOAT


typedef struct {
    int type;
    float fill;
} FluidInfo;

vec4 lightLevelThis;
jobject thisTerrain, thisWall, thisFluid;
vec4 fluidAmountToCol;
vec4 thisTileLuminosity, thisTileOpacity, thisTileOpacity2, sunLight;

const vec4 lightMagic = _mm_set1_ps(8);
const vec4 oneVec = _mm_set1_ps(1);
const vec4 zeroVec = _mm_set1_ps(0);
const vec4 sqrt2Vec = _mm_set1_ps(1.41421356f);

int LIGHTMAP_WIDTH, LIGHTMAP_HEIGHT;
int for_x_start;
int for_y_start;
int for_x_end;
int for_y_end;

void getLightsAndShades(int x, int y, jfloatArray lightmap, jintArray groundmap, jfloatArray lummap, jfloatArray shademap, jbyteArray fluidtypemap, jfloatArray fluidfillmap, jfloatArray sun) {


}

vec4 _mm_darken_ps(vec4 data, vec4 darken) {
    // data * (1f - darken * lightMagic)
    return _mm_mul_ps(
        data,
        _mm_sub_ps(
            oneVec,
            mm_mul_ps(
                darken,
                lightMagic
            )
        )
    );
}

vec4 calculate(int x, int y) {
    getLightsAndShades(x, y);

    lightLevelThis = _mm_max_ps(lightLevelThis, _mm_darken_ps(getLightInternal(x - 1, y - 1), thisTileOpacity2));
    lightLevelThis = _mm_max_ps(lightLevelThis, _mm_darken_ps(getLightInternal(x + 1, y - 1), thisTileOpacity2));
    lightLevelThis = _mm_max_ps(lightLevelThis, _mm_darken_ps(getLightInternal(x - 1, y + 1), thisTileOpacity2));
    lightLevelThis = _mm_max_ps(lightLevelThis, _mm_darken_ps(getLightInternal(x + 1, y + 1), thisTileOpacity2));

    lightLevelThis = _mm_max_ps(lightLevelThis, _mm_darken_ps(getLightInternal(x    , y - 1), thisTileOpacity));
    lightLevelThis = _mm_max_ps(lightLevelThis, _mm_darken_ps(getLightInternal(x    , y + 1), thisTileOpacity));
    lightLevelThis = _mm_max_ps(lightLevelThis, _mm_darken_ps(getLightInternal(x - 1, y    ), thisTileOpacity));
    lightLevelThis = _mm_max_ps(lightLevelThis, _mm_darken_ps(getLightInternal(x + 1, y    ), thisTileOpacity));

    return lightLevelThis
}

void setLightOf(jfloatArray lightmap, int x, int y, vec4 colour) {
    if (y - for_y_start + overscan_open >= 0 &&
        y - for_y_start + overscan_open < LIGHTMAP_HEIGHT &&

        x - for_x_start + overscan_open >= 0 &&
        x - for_x_start + overscan_open < LIGHTMAP_WIDTH) {

        int ypos = y - for_y_start + overscan_open
        int xpos = x - for_x_start + overscan_open

        // unpack our vector
        float[4] vector;
        _mm_store_ps(vector *, colour);

        // set r/g/b/a values
        lightmap[4 * (ypos * LIGHTMAP_WIDTH + xpos)] = vector[0];
        lightmap[4 * (ypos * LIGHTMAP_WIDTH + xpos) + 1] = vector[1];
        lightmap[4 * (ypos * LIGHTMAP_WIDTH + xpos) + 2] = vector[2];
        lightmap[4 * (ypos * LIGHTMAP_WIDTH + xpos) + 3] = vector[3];

        //list[4 * ypos * LIGHTMAP_WIDTH + xpos] = colour
    }
}

vec4 getLightInternal(jfloatArray lightmap, int x, int y) {
    if (y - for_y_start + overscan_open >= 0 &&
        y - for_y_start + overscan_open < LIGHTMAP_HEIGHT &&

        x - for_x_start + overscan_open >= 0 &&
        x - for_x_start + overscan_open < LIGHTMAP_WIDTH) {

        int ypos = y - for_y_start + overscan_open
        int xpos = x - for_x_start + overscan_open

        float r = lightmap[4 * (ypos * LIGHTMAP_WIDTH + xpos)];
        float g = lightmap[4 * (ypos * LIGHTMAP_WIDTH + xpos) + 1];
        float b = lightmap[4 * (ypos * LIGHTMAP_WIDTH + xpos) + 2];
        float a = lightmap[4 * (ypos * LIGHTMAP_WIDTH + xpos) + 3];

        // TODO check rgba argument order (RGBA or ABGR, intel doc is confusing)
        return _mm_set_ps(r, g, b, a);
    }
}

JNIEXPORT void JNICALL Java_net_torvald_terrarum_worlddrawer_LightmapRenderer_fireRecalculateEventJNI
    (JNIEnv * env, jobject object, jint forxstart, jint forxend, jint forystart, jint foryend, jint lighmapwidth, jint lightmapheight, jfloatArray lightmap, jintArray groundmap, jfloatArray lummap, jfloatArray shademap, jbyteArray fluidtypemap, jfloatArray fluidfillmap, jfloatArray sun) {    // update (read) variables

    LIGHTMAP_WIDTH = lightmapwidth;
    LIGHTMAP_HEIGHT = lightmapheight;
    for_x_start = forxstart;
    for_x_end = forxend;
    for_y_start = forystart;
    for_y_end = foryend;

    /**
     * Updating order:
     * ,--------.   ,--+-----.   ,-----+--.   ,--------. -
     * |↘       |   |  |    3|   |3    |  |   |       ↙| ↕︎ overscan_open / overscan_opaque
     * |  ,-----+   |  |  2  |   |  2  |  |   +-----.  | - depending on the noop_mask
     * |  |1    |   |  |1    |   |    1|  |   |    1|  |
     * |  |  2  |   |  `-----+   +-----'  |   |  2  |  |
     * |  |    3|   |↗       |   |       ↖|   |3    |  |
     * `--+-----'   `--------'   `--------'   `-----+--'
     * round:   1            2            3            4
     * Run in this order: 0-2-3-4-1
     * zero means we wipe out lightmap.
     * But why start from 2? No special reason.
     */

    // clear the lightmap
    for (size_t i = 0; i < sizeof(lightmap) / sizeof(lightmap[0]); i++) {
        lightmap[i] = 0f;
    }

    return;
}
