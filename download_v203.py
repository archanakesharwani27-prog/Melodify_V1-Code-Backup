import urllib.request
import re

url = "https://www.amazon.com/photos/shared/hy4wOzGjQ1ulzN40E-q6Bw.wE8KYzZ7rGqD7h-l4vKKHe"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
html = urllib.request.urlopen(req).read().decode('utf-8')

thumbnail_urls = list(set(re.findall(r'https://thumbnails-photos\.amazon\.com/v1/thumbnail/[^\s"\'<>]+', html)))
print("Found unique thumbnail URLs:", len(thumbnail_urls))

for i, t in enumerate(thumbnail_urls):
    full_url = re.sub(r'viewBox=\d+%2C\d+', 'viewBox=1920%2C1920', t)
    print(f"Downloading {i}:", full_url)
    try:
        req2 = urllib.request.Request(full_url, headers={'User-Agent': 'Mozilla/5.0'})
        data = urllib.request.urlopen(req2).read()
        filename = f"amazon_rejection_v203_{i}.png"
        with open(filename, "wb") as f:
            f.write(data)
        print("Saved to", filename, "size:", len(data))
    except Exception as e:
        print("Error:", e)
