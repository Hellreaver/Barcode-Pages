# Changelog

Newest first. Write each change for the person installing the app under "Unreleased", one
"- " line per change; indent continuation lines with two spaces. When GitHub Actions publishes a
release it renames "Unreleased" to that version, uses the lines as the release notes, and
commits the file back. Editing an older section updates that release's notes on the next build.

## Unreleased

- Fixed bugs in some edge cases

## 1.17

- Release pages now describe each change in plain words, and every earlier release got its
  notes written up the same way

## 1.16

- No change to the app. The release build uses current versions of GitHub's build tools

## 1.15

- Versions now read 1.15, 1.16 and so on: two digits after the dot, one step per release, with
  no skipped numbers. 1.15 installs over 1.0.14

## 1.0.14

- Same app as 1.0.13, rebuilt after the project's history was cleaned up for going public

## 1.0.13

- No change to the app. The project files no longer name the store chain, and the tests cover
  more trip label codes

## 1.0.12

- The trip label example on the first screen starts folded. Tap "Trip label example" to open it

## 1.0.11

- Trip labels work. A code starting with TL keeps its trip id in lowercase, the way the label
  prints and encodes it, so typing TL0A1B-2 gives TL0a1b-2
- The hint on the first screen names the Dispense Order list as another place to read codes

## 1.0.10

- The example label's top-right symbol is split corner to corner: the bag symbol on one half,
  the no-bags symbol on the other

## 1.0.9

- Until the first code is entered, the first screen shows a drawing of a tote label with the
  code to type highlighted. Its bars are decoration and can't be scanned

## 1.0.8

- The hint and the empty box say where the code is: the bottom-right corner of the tote label,
  like Z13334

## 1.0.7

- No change to the app. Release pages list what changed since the previous release

## 1.0.6

- Fixed: on Samsung's keyboard (Galaxy Note9) the first letter typed into a box disappeared

## 1.0.5

- The download is named after its version, like Barcode-Pages-1.0.5.apk, so copies in Downloads
  can be told apart

## 1.0.4

- Letters switch to capitals as you type, because the barcode has to match the label exactly
- On launch the app checks GitHub for a newer version. When one exists, the Share button turns
  into Update

## 1.0.3

- The app is now called Barcode-Pages and is signed with a new private key. It installs as a
  separate app from Tote Barcodes 1.0.1, so uninstall that one

## 1.0.1

- First release, named Tote Barcodes. Type the codes of the totes that didn't scan out, tap
  Next, and scan large Code 128 barcodes straight off the screen
- The barcode screen keeps the phone awake at full brightness, and each tote has a Mark scanned
  button
- Share screen with a QR code and a Send link button for the download page
