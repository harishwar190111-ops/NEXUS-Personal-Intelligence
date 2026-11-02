# Approach B - genuine NEXUS firmware: feasibility notes

**Verdict: not proposed. Nothing here justifies touching the device.** NEXUS TV is an app/launcher; it is not an operating system and must never be described as one.

## What is known (third-party sources, not verified on your unit)
- DVM4KA01 is the Motorola-branded "4K Android TV Stick", sold in India through Flipkart from March 2021 (licensed brand). It shipped with Android 9 and later received Android 10.
- Listings give 2 GB RAM, 16 GB storage, a quad-core Cortex-A53 CPU and Mali-G31 MP2 GPU. One database lists the SoC as **Amlogic S905Y2**; retailers/press do not name it. Treat the SoC as **unconfirmed**.

## What NEXUS TV can confirm on-device
Settings, "Device information" reads Build.HARDWARE, BOARD, DEVICE, PRODUCT, BOOTLOADER version, build, ABIs, RAM and display straight from the stick. Record those values here: **Hardware / Board: ______**.

## What is still unknown (and why a firmware project is blocked)
| Requirement | Status |
|---|---|
| Exact SoC and board | Unconfirmed until the device-info screen is read |
| Bootloader unlock | Unknown. Cannot be tested from an app; needs fastboot/USB access that this stick does not clearly offer. Not attempted |
| AOSP device tree / kernel source | Not found for this model. Amlogic BSP kernels exist, but a matching vendor tree for DVM4KA01 was not located |
| Vendor binaries, Wi-Fi/Bluetooth/HDMI/IR drivers | Not available |
| Verified boot / signing | Retail Android TV devices normally enforce it; no unlock path verified |
| Recovery / stock firmware restore | No restore image located; a failed flash could brick the only TV box |
| Manufacturer support | Licensed-brand device; no developer programme found |

## Recommendation
Stay on Approach A (an app, optionally a Home app). Revisit only if all of these become verified: unlock path, matching source/vendor blobs, a factory restore image, and a spare device to risk. No flashing, bootloader or partition work will be attempted from NEXUS.
