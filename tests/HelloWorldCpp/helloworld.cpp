/******************************************************************************
 * Sample comment block for an example project with errors.
 *****************************************************************************/
#include <iostream>
using namespace std;
#include "Planet.h"

int main()
{
   Planet planet("Earth");
   cout << "Hello " << planet.getName() << "!\n";
   return 0;
}
