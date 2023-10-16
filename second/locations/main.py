import asyncio

import console
from console import console
from controllers.apicontroller import ApiController


async def main():
    controller = ApiController()

    place_name = console.get_place_name()
    locations = await controller.find_locations(place_name)
    console.print_places(locations)

    chosen_ind = console.get_place_ind(len(locations))
    location = locations[chosen_ind]
    weather, places = await controller.find_weather_and_interesting_places(location)
    console.print_weather(location['name'], weather)


if __name__ == "__main__":
    asyncio.run(main())
