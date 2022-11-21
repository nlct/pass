#include "Planet.h"

Planet::Planet(const char* planetName)
{
  strncpy(name, planetName, sizeof(name));
  name[sizeof(name)] = 0;
}
const char* Planet::getName()
{
  return name;
}
