# Media Information & MKV tools

> Read this first for: Media Information screen, metadata probing, MKV/EBML reading & writing (tracks, chapters, attachments, embedded fonts).
> Parent index: [../brain.md](../brain.md). Line numbers below are approximate; if they drift, grep the symbol name.

## Files

| File | Lines | Role |
|---|---|---|
| `ui/screens/MediaInformationScreen.kt` | 1811 | Media Information screen (codec/resolution/tracks/chapters/attachments), reached from the library long-press menu; also extract/export fonts, subtitles, chapters. |
| `util/MediaMetadataExtractor.kt` | 1065 | Single live metadata extractor (`ExtractedMediaMetadata`): resolution/fps/codec/subtitle-track probing. |
| `util/MkvManagerEngine.kt` | 3054 | Hand-rolled MKV/EBML reader + writer (`MkvTrackInfo`, `MkvChapterInfo`, `MkvAttachmentInfo`, `MkvContainerMetadata`, `EbmlStreamReader`, `EbmlStreamWriter`). Largest file in util/. |

## Flow

- HomeScreen long-press → MediaInformationScreen → MediaMetadataExtractor + MkvManagerEngine → UI sections (`MediaInfoSection`).
- VideoFolderScanner also calls MediaMetadataExtractor while scanning; MpvPlayerController uses MkvManagerEngine for embedded subtitle/attachment extraction.

## Edit map (need → where)

| If you need to… | Edit |
|---|---|
| Info screen layout | MediaInformationScreen.kt |
| Codec / fps / track probing | MediaMetadataExtractor.kt |
| MKV read/write, chapters, attachments | MkvManagerEngine.kt |

## Rules / gotchas

- MkvManagerEngine keeps the EBML element-ID constant table (`ID_*`) on purpose as a reference; unused IDs are harmless.
- MediaInformationScreen.kt contains a brace/quote pattern some linters flag; edit carefully and re-verify balance.

## Talks to other areas

- Depends on: [design-system](../design-system/brain.md), [library-home](../library-home/brain.md), [settings](../settings/brain.md), [subtitles](../subtitles/brain.md)
- Used by: [app-shell](../app-shell/brain.md), [library-home](../library-home/brain.md), [playlists-history](../playlists-history/brain.md), [video-player](../video-player/brain.md)

## Symbol index (functions, `name:line`) for large files

**`ui/screens/MediaInformationScreen.kt`**: MediaInformationScreen:143, copyText:267, shareText:273, queueExtract:281, MediaInfoContent:616, OverviewCard:911, TrackCard:1044, SubtitleTrackCard:1125, ChapterInfoCard:1173, AttachmentCard:1266, ExtractButton:1300, ActionRow:1326, SmallAction:1334, InfoBadge:1361, InfoCard:1385, InfoGrid:1450, EmptyInfo:1462, subtitleFormatLabel:1468, videoExtension:1480, audioExtension:1493, subtitleExtension:1516, channelLabel:1532, formatBytes:1542, formatMs:1555, formatBitrate:1565, sanitizeFileComponent:1573, mapLanguageToSimpleName:1579, detectLanguageFromTitle:1652, resolveTrackLanguageName:1669, buildTrackOutputFileName:1678, getUniqueDestinationFile:1701, buildInformationText:1737, buildChapterText:1784

**`util/MediaMetadataExtractor.kt`**: extractMetadata:53, refineCalculatedFps:300, extractFramerateFromFormat:315, isSubtitleMime:347, normalizeSubtitleCodec:367, formatSubtitleCounts:391, formatResolution:405, scanExternalSubtitleFiles:432, parseMkvContainer:487, parseMkvTracks:587, parseMkvAttachedFileName:683, parseMkvSegmentInfo:717, readEbmlId:749, readEbmlVint:772, readEbmlUint:792, parseMp4Container:814, parseMp4Moov:867, parseMp4Trak:920

**`util/MkvManagerEngine.kt`**: formattedStreamSize:172, formatBytes:290, codecDisplayName:303, parseHevcCodecPrivate:338, parseAvcCodecPrivate:390, openSafeInputStream:483, openSafeOutputStream:493, parseMkvFile:505, extractSingleTrack:660, extractMediaTrack:719, formatAssTime:772, extractSubtitleTrackToString:786, extractSubtitleTrack:931, extractAttachment:950, exportChapters:967, readSubtitleText:1016, remuxMkv:1039, prepareSubtitleMuxBlocks:1174, writeAllSubtitleClusters:1231, parseSimpleTextCues:1271, resolveCueDurations:1315, extractTextCuesFromEbml:1348, formatSrtTime:1363, formatVttTime:1372, xmlEscape:1381, formatXmlTime:1388, parseMkv:1405, parseEbmlHeaderInfo:1427, parseSegmentChildren:1439, streamSelectedClusters:1474, streamSegmentClusters:1496, isTopLevelSegmentElement:1743, isUnknownSize:1754, formatMkvDateUtc:1763, parseInfo:1775, parseTracks:1797, parseTrackEntry:1813, parseChapters:2053, parseChapterAtom:2078, parseAttachments:2119, parseAttachedFile:2135, extractBlocksForTrack:2167, extractSegmentBlocksForTrack:2182, parseCueFromPayload:2285, addExtractedCue:2370, parseKaraokeDurationCentis:2433, skipUnknownSizeCluster:2443, scanTrackByteSizes:2471, scanSegmentForTrackBytes:2488, unreadElement:2548, readByte:2553, readElementId:2559, readElementSize:2587, readUnsignedInt:2611, readSignedInt:2621, readFloat:2639, readString:2649, readBinary:2654, readBinaryExact:2670, skipBytes:2685, readVintTrackHeader:2698, resyncToNextCluster:2720, flush:2750, openSegment:2754, currentSegmentOffset:2762, recordCuePoint:2766, writeEbmlHeader:2772, writeSegmentInformation:2784, writeTracksElement:2803, writeTagsElement:2895, writeChaptersElement:2916, writeAttachmentsElement:2935, writeMasterElement:2950, writeRawBytesWithId:2962, writeUnsignedInt:2969, writeSignedInt:2980, writeFloat:2995, writeString:3001, writeBinary:3006, writeElement:3010, writeElementId:3017, writeElementSize:3029
