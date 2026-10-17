# Being Muslim

The shared product vocabulary for the Being Muslim apps — an Islamic mobile
product that treats religious-content accuracy, attribution, and respectful
presentation as critical quality. These are the canonical terms used across both
native apps and the specification documents. "Being Muslim" is a working name.

## Language

### Quran content

**Quran text**:
The Arabic text of the Quran exactly as published by an identified source. Never
paraphrased, normalized, or edited.
_Avoid_: Quran, original text, verses

**Surah**:
A chapter of the Quran, kept together with the source's number and name.
_Avoid_: Chapter, section

**Ayah**:
A verse within a surah, kept together with the source's number.
_Avoid_: Verse, line

**Translation**:
A rendering of the meaning of source text into another language by an identified
translator and edition, always shown as distinct from the Arabic text.
_Avoid_: Interpretation, version, "the Quran in English"

**Transliteration**:
A representation of Arabic sounds in another script, usually Latin. It is neither
the Arabic text nor a translation.
_Avoid_: Romanization, phonetic text

**Tafsir**:
Explanatory commentary on the Quran attributed to an identified author or source.
_Avoid_: Translation, notes

**Commentary**:
Explanatory notes written by the project about a passage. Distinct from tafsir,
which is attributed to a source.
_Avoid_: Tafsir, editorial notes

**Orthographic normalization**:
A documented transformation of source text, such as a diacritic convention.
Permitted only when its source and transformation are recorded.
_Avoid_: Cleanup, correction, fixing

### Hadith and dua

**Hadith**:
A report attributed to the Prophet, presented with its collection and reference.
_Avoid_: Saying, tradition, narration

**Collection**:
The named source work a hadith or dua is drawn from.
_Avoid_: Book, corpus

**Grading**:
An authenticity assessment supplied by a traceable source, always shown together
with whose grading it is.
_Avoid_: Rating, verification, authenticity

**Dua**:
A reported supplication, presented with its source. Different reported wordings
are kept separate.
_Avoid_: Prayer, invocation, supplication

### Prayer times and qibla

**Prayer**:
One of the five daily ritual prayers, which is what the app calculates times for.
_Avoid_: Dua, salah, namaz

**Prayer time**:
The calculated time of a prayer at a given location and date.
_Avoid_: Salah time, namaz time

**Calculation method**:
The named set of parameters used to compute prayer times, including angles,
intervals, high-latitude rules, the Asr method, and offsets.
_Avoid_: Formula, algorithm, default method

**Juristic method**:
The convention used for the Asr calculation and other method parameters. A
visible, user-chosen setting that is never changed silently.
_Avoid_: Madhab, school

**High-latitude rule**:
The convention applied when a location's latitude leaves true dawn or dusk
undefined.
_Avoid_: Adjustment, fallback

**Offset**:
A user-applied adjustment to a calculated prayer time, kept separate from the raw
calculation.
_Avoid_: Correction, fudge

**Qibla**:
The direction from a location toward the Kaaba, computed from coordinates.
_Avoid_: Bearing, compass direction

**Location**:
A place used for prayer-time and qibla calculations, taken from the device or
entered manually.
_Avoid_: Coordinates, GPS

### Dates

**Hijri calendar**:
The Islamic lunar calendar. Whether a given date is calculated, published, or
observed must be stated.
_Avoid_: Islamic calendar, lunar calendar

**Moon sighting**:
A local, announced start of a Hijri month. A date based on one is not equivalent
to a calculated Hijri date.
_Avoid_: Observation, sighting

### Reminders

**Reminder**:
A user-configured prompt to observe a practice, delivered as a platform
notification.
_Avoid_: Alarm, notification

**Notification**:
The operating-system delivery mechanism for reminders. Its exact delivery time is
not guaranteed.
_Avoid_: Reminder, push

### Content governance

**Content source**:
The identified publisher and edition or version that content was obtained from,
recorded with the content itself.
_Avoid_: Provider, dataset, feed

**Provenance**:
The recorded origin of content: source, edition or version, license, and
retrieval date.
_Avoid_: Metadata, attribution

**Attribution**:
The credit shown for the author, translator, or publisher of content.
_Avoid_: Citation, provenance, credit

**Review owner**:
The person accountable for reviewing language, references, or religious metadata
before a content source is published.
_Avoid_: Approver, maintainer

**Correction**:
A traceable change to published content after an error is confirmed. Corrections
are versioned, never silent.
_Avoid_: Fix, edit, patch

**Generated content**:
Text produced by an AI or algorithmic feature, always labelled and never shown as
source text or a scholarly ruling.
_Avoid_: Answer, summary, AI text
