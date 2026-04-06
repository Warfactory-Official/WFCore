import json
import matplotlib.pyplot as plt
import matplotlib.patches as patches
import matplotlib.ticker as ticker
# Python is amazing at quick drawing
with open("mock.json", "r") as f:
    mock_raw = json.load(f)
with open("clusters.json", "r") as f:
    clusters = json.load(f)
machines_x, machines_z, machines_v = [], [], []
players_x, players_z = [], []
for key_str, data in mock_raw.items():
    coords = key_str.strip("()").split(", ")
    x = int(coords[0])
    z = int(coords[1])
    if data["type"] == "STRUCTURE":
        machines_x.append(x)
        machines_z.append(z)
        machines_v.append(data["value"])
    else:
        players_x.append(x)
        players_z.append(z)
fig, ax = plt.subplots(figsize=(14, 14))
scatter_machines = ax.scatter(
    machines_x,
    machines_z,
    c=machines_v,
    cmap="plasma",
    s=20,
    zorder=3,
    label="Machines",
)
cbar = plt.colorbar(scatter_machines, ax=ax, shrink=0.6, pad=0.02)
cbar.set_label("Machine Value (TargetType.STRUCTURE)")
ax.scatter(
    players_x,
    players_z,
    c="cyan",
    marker="^",
    edgecolor="black",
    s=60,
    label="Players",
    zorder=4,
)
for c in clusters:
    b_min = c["boundingBox"]["min"]
    b_max = c["boundingBox"]["max"]
    w = b_max["x"] - b_min["x"]
    h = b_max["z"] - b_min["z"]
    rect = patches.Rectangle(
        (b_min["x"], b_min["z"]),
        w,
        h,
        linewidth=1.5,
        edgecolor="red",
        facecolor="none",
        zorder=5,
        label="Detected Base" if c == clusters[0] else "",
    )
    ax.add_patch(rect)

    center = c["centerPoint"]
    ax.plot(
        center["x"],
        center["z"],
        "rx",
        markersize=10,
        zorder=6,
        label="Cluster Center" if c == clusters[0] else "",
    )

ax.xaxis.set_major_locator(ticker.MultipleLocator(16))
ax.yaxis.set_major_locator(ticker.MultipleLocator(16))
ax.grid(which="major", color="gray", linestyle="-", linewidth=0.2, alpha=0.4)
ax.set_aspect("equal")
ax.set_title("Radar Mock World Visualization (16x16 Chunk Grid)")
ax.set_xlabel("X Coordinate")
ax.set_ylabel("Z Coordinate")
ax.legend(loc="upper right")
plt.tight_layout()
plt.show()
