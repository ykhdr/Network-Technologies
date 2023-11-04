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
        interesting_places_cour = self._interesting_places_controller.find_interesting_places(lat, lon)

        interesting_places = (await interesting_places_cour)['results']
        places_id = []
        for i in range(min(5, len(interesting_places))):
            places_id.append(interesting_places[i]['id'])

        descriptions_cour = self._description_places_controller.find_descriptions_for_places(places_id)

        descriptions = []
        for desc in descriptions_cour:
            descriptions.append(await desc)

        return await weather, descriptions


class _InterestingPlacesController:
    def __init__(self):
        self._url = 'https://kudago.com/public-api/v1.4/places'

    def find_interesting_places(self, lat, lon):
        query = {
            "lang": "ru",
            "page_size": 5,
            "lon": lon,
            "lat": lat,
            "fields": ["id"],
            "radius": 1000
        }

        return get_request(self._url, query)


class _LocationController:
    _url = 'https://graphhopper.com/api/1/geocode'

    def find_locations(self, name: string):
        query = {
            "q": name,
            "locale": "ru",
            "limit": "5",
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
        self._url = 'https://kudago.com/public-api/v1.2/places/'

    def find_descriptions_for_places(self, ids):
        query = {
        }

        descriptions = []

        for id_ in ids:
            url = self._url + str(id_)
            descriptions.append(get_request(url, query))

        return descriptions
