import string
import os
import struct

from api.request import get_request


class ApiController:
    def __init__(self):
        self._location_controller = _LocationController()
        self._weather_controller = _WeatherController()
        self._interesting_places_controller = _InterestingPlacesController()
        self._description_places_controller = _DescriptionPlacesController()

    async def find_locations(self, name: string):
        return (await self._location_controller.find_locations(name))['hits']

    async def find_weather_and_interesting_places(self, location):
        lat = location['point']['lat']
        lon = location['point']['lng']
        weather = self._weather_controller.find_weather(lat, lon)

        return await weather, 0


class _InterestingPlacesController:
    def __init__(self):
        self._url = 'https://opentripmap.io/docs#/Objects%20list/getListOfPlacesByRadius'


class _LocationController:
    _url = 'https://graphhopper.com/api/1/geocode'

    def find_locations(self, name: string):
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

        return get_request(self._url, query)


class _WeatherController:
    _url = 'https://api.openweathermap.org/data/2.5/weather'

    def find_weather(self, lat, lon):
        query = {
            "lat": lat,
            "lon": lon,
            "units": "metric",
            "appid": os.getenv('WEATHER_KEY')
        }

        return get_request(self._url, query)


class _DescriptionPlacesController:
    def __init__(self):
        self._url = 'https://opentripmap.io/docs#/Object%20properties/getPlaceByXid'
