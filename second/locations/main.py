import asyncio

from controllers.apicontroller import ApiController


async def main():
    print('Enter name:')
    s = input()
    controller = ApiController()

    res = await controller.find_places(s)
    for i, item in enumerate(res):
        print(f'[{i+1}] {item["name"]}, {item["country"]}, points: {item["point"]["lat"]}, {item["point"]["lng"]} ')


if __name__ == "__main__":
    asyncio.run(main())
