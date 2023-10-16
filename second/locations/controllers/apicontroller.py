import string
import os

from api.request import get_request


class ApiController:
    def __init__(self):
        self._location_controller = _LocationController()
        self._weather_controller = _WeatherController()
        self._interesting_places_controller = _InterestingPlacesController()
        self._description_places_controller = _DescriptionPlacesController()

    def find_places(self, name: string):
        return self._location_controller.find_places(name)


class _InterestingPlacesController:
    def __init__(self):
        self._url = 'https://opentripmap.io/docs#/Objects%20list/getListOfPlacesByRadius'


class _LocationController:
    _url = 'https://graphhopper.com/api/1/geocode'

    def __init__(self):
        self._latest_places = {}

    async def find_places(self, name: string):
        query = {
            "q": name,
            "locale": "en",
            "limit": "5",
            "reverse": "false",
            "debug": "false",
            "point": "45.93272,11.58803",
            "provider": "default",
            "key": os.getenv('LOCATION_KEY')
        }

        places = (await get_request(self._url, query))['hits']
        self._latest_places = places
        return places


class _WeatherController:
    def __init__(self):
        self._url = 'https://api.openweathermap.org/data/2.5/weather'


class _DescriptionPlacesController:
    def __init__(self):
        self._url = 'https://opentripmap.io/docs#/Object%20properties/getPlaceByXid'
