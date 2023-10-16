from textwrap import dedent


def get_place_name():
    print('Enter name:')
    s = input()
    print()
    return s


def print_places(places):
    print("Search result:")
    for i, item in enumerate(places):
        print(f'[{i + 1}] {item["name"]}, {item["country"]}, points: {item["point"]["lat"]}, {item["point"]["lng"]} ')
    print()


def get_place_ind(max_places):
    print('Choose number of state:')
    while True:
        s = input()

        if s.isdigit() and 0 < int(s) <= max_places:
            print()
            return int(s) - 1
        else:
            print('Unknown number of place, please choose one from list')


def print_weather(location_name, weather):
    print(dedent(f"""\
        ----Current weather in {location_name}----
        Main:
            {weather['weather'][0]['main']}: {weather['weather'][0]['description']}
        Temperature:
            {weather['main']['temp']}°, feels like {weather['main']['feels_like']}
            Min: {weather['main']['temp_min']}, Max: {weather['main']['temp_max']} 
        Atmospheric pressure:  {weather['main']['pressure']} Pha
        Visibility: {weather['visibility']} meters
        Wind:
            Speed: {weather['wind']['speed']} meter/sec
            Direction: {weather['wind']['deg']}° 
    """))
