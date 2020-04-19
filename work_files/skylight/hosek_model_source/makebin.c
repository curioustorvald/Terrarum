#include <stdio.h>
#include <string.h>
#include "ArHosekSkyModelData_Spectral.h"
#include "ArHosekSkyModelData_CIEXYZ.h"
#include "ArHosekSkyModelData_RGB.h"

double testset[] = {
    1.0,
    2.0,
    3.0,
    4.0
};

void double_to_char(double a, char outbuf[]) {
    memcpy(outbuf, &a, sizeof(a));
}

int main(int argc, char const *argv[])
{
    int i = 0;
    FILE * outfile;

    outfile = fopen("./datasetRGBRad3.bin", "w");

    for (i = 0; i < sizeof(datasetRGBRad3) / sizeof(double); i++) {
        double test_num = datasetRGBRad3[i];

        char outchars[sizeof(test_num)];

        double_to_char(test_num, outchars);

        int k = 0;
        for (k = 0; k < sizeof(test_num); k++) {
            fputc(outchars[k], outfile);
            printf("%02x ", outchars[k]);
        }
        fflush(outfile);
        printf("\n");
        printf("Writing entry %d\n", i + 1);
    }
    
    fflush(outfile);
    fclose(outfile);
    printf("Operation completed successfully.\n");

    /**/

    return 0;
}