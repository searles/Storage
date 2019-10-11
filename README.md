# Storage

Activities and classes for a manager of eg files or bookmark entries.
Basically a recyclerview that operates on Strings as keys, allows
multi-selection and filtering.

## Technically

This project aims to solve its task as much the android way as possible. It involves

* SelectionTracker that uses String as keys, hence supporting frequent reordering of entries
* Theme-based colors for the selection
* ViewModelProvider and LiveData that provides the data that are shown
* Filtering entries in a RecyclerView using text input
    + Highlighting the matched parts in each entry