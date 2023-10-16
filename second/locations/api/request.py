import logging

import aiohttp


async def get_request(url, query):
    async with aiohttp.ClientSession() as session:
        async with session.get(url, params=query) as response:
            logging.info(f'Reqest status on url:{url} is {response.status}')

            return await response.json()
