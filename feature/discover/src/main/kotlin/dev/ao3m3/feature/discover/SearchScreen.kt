package dev.ao3m3.feature.discover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import dev.ao3m3.core.data.net.Ao3Exception
import dev.ao3m3.core.designsystem.component.EmptyState
import dev.ao3m3.core.designsystem.component.MaintenanceBanner
import dev.ao3m3.core.designsystem.component.SheetLayout
import dev.ao3m3.core.designsystem.component.SkeletonList
import dev.ao3m3.core.designsystem.component.WorkCard
import dev.ao3m3.core.designsystem.icon.Ao3Icons
import dev.ao3m3.core.designsystem.theme.Spacing
import dev.ao3m3.core.model.SearchFilters
import dev.ao3m3.core.model.SortOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onOpenWork: (Long) -> Unit,
    initialFandom: String? = null,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val savedSearches by viewModel.savedSearches.collectAsStateWithLifecycle()
    val savedFandoms by viewModel.savedFandoms.collectAsStateWithLifecycle()
    val fandomSuggestions by viewModel.fandomSuggestions.collectAsStateWithLifecycle()
    val results = viewModel.results.collectAsLazyPagingItems()

    // Arriving from a work's fandom chip: search that fandom straight away.
    LaunchedEffect(initialFandom) {
        if (!initialFandom.isNullOrBlank()) viewModel.searchFandom(initialFandom)
    }

    var text by rememberSaveable { mutableStateOf(query.query) }
    var filtersOpen by rememberSaveable { mutableStateOf(false) }
    var sortOpen by rememberSaveable { mutableStateOf(false) }
    var savedOpen by rememberSaveable { mutableStateOf(false) }

    val refresh = results.loadState.refresh
    val maintenance = (refresh as? LoadState.Error)?.error is Ao3Exception.Maintenance

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search") },
                actions = {
                    IconButton(onClick = { savedOpen = true }) {
                        Icon(Ao3Icons.Bookmark, contentDescription = "Saved searches")
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.gutter, vertical = Spacing.sm),
                placeholder = { Text("Title, author, or tag") },
                shape = CircleShape,
                singleLine = true,
                leadingIcon = { Icon(Ao3Icons.Search, contentDescription = null) },
                trailingIcon = {
                    if (text.isNotEmpty()) {
                        IconButton(onClick = { text = ""; viewModel.submit("") }) {
                            Icon(Ao3Icons.Close, contentDescription = "Clear search")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.submit(text) }),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.gutter),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AssistChip(
                    onClick = { filtersOpen = true },
                    label = {
                        Text(
                            if (query.filters.isEmpty) "Filters"
                            else "Filters · ${query.filters.activeCount}",
                        )
                    },
                    leadingIcon = { Icon(Ao3Icons.Filter, contentDescription = null) },
                )
                AssistChip(
                    onClick = { sortOpen = true },
                    label = { Text(query.sort.label) },
                    leadingIcon = { Icon(Ao3Icons.Sort, contentDescription = null) },
                )
            }

            // Saved fandoms: one tap searches that fandom, the X forgets it. Applying a fandom
            // filter in the sheet adds it here, so saving never needs its own dialog.
            if (savedFandoms.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = Spacing.gutter,
                        end = Spacing.gutter,
                        top = Spacing.xs,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(savedFandoms, key = { it }) { fandom ->
                        InputChip(
                            selected = query.filters.fandom.equals(fandom, ignoreCase = true),
                            onClick = { viewModel.searchFandom(fandom) },
                            label = {
                                Text(
                                    text = fandom,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Ao3Icons.Close,
                                    contentDescription = "Remove saved fandom $fandom",
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { viewModel.removeFandom(fandom) },
                                )
                            },
                        )
                    }
                }
            }

            if (maintenance) {
                MaintenanceBanner(message = "The Archive is offline. Showing saved results.")
            }

            val idle = query.query.isBlank() && query.filters.isEmpty
            when {
                // Nothing searched yet: show the prompt instead of a skeleton that implies the
                // first page is on its way.
                idle -> EmptyState(
                    icon = Ao3Icons.Search,
                    title = "Find something to read",
                    message = "Search by title, author, fandom or tag.",
                )

                refresh is LoadState.Loading && results.itemCount == 0 -> SkeletonList(rows = 5)

                refresh is LoadState.Error && results.itemCount == 0 -> {
                    EmptyState(
                        icon = Ao3Icons.Offline,
                        title = if (maintenance) "Archive in maintenance" else "Search unavailable",
                        message = refresh.error.message ?: "Couldn't load results.",
                        actionLabel = "Try again",
                        onAction = { results.retry() },
                    )
                }

                results.itemCount == 0 && refresh is LoadState.NotLoading -> {
                    EmptyState(
                        icon = Ao3Icons.Search,
                        title = "No works found",
                        message = "Try a broader tag or fewer filters.",
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = Spacing.sm, bottom = Spacing.md),
                    ) {
                        items(
                            count = results.itemCount,
                            key = { index -> results.peek(index)?.id ?: index },
                        ) { index ->
                            results[index]?.let { work ->
                                WorkCard(
                                    work = work,
                                    onClick = { onOpenWork(work.id) },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                        val appendState = results.loadState.append
                        if (appendState is LoadState.Loading) {
                            item { SkeletonList(rows = 2) }
                        } else if (appendState is LoadState.Error) {
                            item {
                                AppendError(
                                    message = appendState.error.message
                                        ?: "Couldn't load more results.",
                                    onRetry = { results.retry() },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (filtersOpen) {
        FilterSheet(
            initial = query.filters,
            suggestions = fandomSuggestions,
            onFandomInput = viewModel::suggestFandoms,
            onSaveFandom = viewModel::saveFandom,
            onDismiss = { filtersOpen = false },
            onApply = {
                viewModel.applyFilters(it)
                filtersOpen = false
            },
        )
    }

    if (sortOpen) {
        SortSheet(
            current = query.sort,
            onDismiss = { sortOpen = false },
            onSelect = {
                viewModel.setSort(it)
                sortOpen = false
            },
        )
    }

    if (savedOpen) {
        ModalBottomSheet(onDismissRequest = { savedOpen = false }) {
            SavedSearchesSheet(
                savedSearches = savedSearches,
                currentQuery = text,
                onSave = { viewModel.saveSearch(text) },
                onSelect = { saved ->
                    text = saved
                    viewModel.submit(saved)
                    savedOpen = false
                },
                onRemove = viewModel::removeSavedSearch,
                onDismiss = { savedOpen = false },
            )
        }
    }
}

@Composable
private fun AppendError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.gutter, vertical = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.sm))
        TextButton(onClick = onRetry) { Text("Load more") }
    }
}

@Composable
private fun SavedSearchesSheet(
    savedSearches: List<String>,
    currentQuery: String,
    onSelect: (String) -> Unit,
    onRemove: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    SheetLayout(
        title = "Saved searches",
        footer = {
            TextButton(onClick = onSave, enabled = currentQuery.isNotBlank()) {
                Text("Save current search")
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    ) {
        if (savedSearches.isEmpty()) {
            Text(
                text = "No saved searches yet. Type a search, then use \"Save current search\" " +
                    "here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = Spacing.lg),
            )
        } else {
            savedSearches.forEach { saved ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(saved) }
                        .padding(vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = saved,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { onRemove(saved) },
                        modifier = Modifier.size(Spacing.touch),
                    ) {
                        Icon(
                            imageVector = Ao3Icons.Close,
                            contentDescription = "Remove saved search",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(
    initial: SearchFilters,
    suggestions: List<String>,
    onFandomInput: (String) -> Unit,
    onSaveFandom: (String) -> Unit,
    onDismiss: () -> Unit,
    onApply: (SearchFilters) -> Unit,
) {
    var fandom by remember { mutableStateOf(initial.fandom) }
    var savedFandom by remember { mutableStateOf<String?>(null) }
    var relationship by remember { mutableStateOf(initial.relationship) }
    var character by remember { mutableStateOf(initial.character) }
    var otherTags by remember { mutableStateOf(initial.otherTags) }
    var minWords by remember { mutableStateOf(initial.minWords?.toString().orEmpty()) }
    var maxWords by remember { mutableStateOf(initial.maxWords?.toString().orEmpty()) }
    var completeOnly by remember { mutableStateOf(initial.completeOnly) }
    var singleChapter by remember { mutableStateOf(initial.singleChapterOnly) }
    var noCrossovers by remember { mutableStateOf(initial.excludeCrossovers) }
    // Compose state, not a mutable set: mutating a MutableSet in place never triggers a
    // recomposition, which is why the chips only appeared to toggle after Apply.
    var ratings by remember { mutableStateOf(initial.rating) }
    var warnings by remember { mutableStateOf(initial.warnings) }
    var categories by remember { mutableStateOf(initial.categories) }
    val invalidRange = minWords.toIntOrNull()?.let { min ->
        maxWords.toIntOrNull()?.let { max -> min > max }
    } ?: false

    ModalBottomSheet(onDismissRequest = onDismiss) {
        SheetLayout(
            title = "Filters",
            footer = {
                TextButton(
                    onClick = {
                        fandom = ""
                        relationship = ""
                        character = ""
                        otherTags = ""
                        minWords = ""
                        maxWords = ""
                        completeOnly = false
                        singleChapter = false
                        noCrossovers = false
                        ratings = emptySet()
                        warnings = emptySet()
                        categories = emptySet()
                    },
                ) { Text("Reset") }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        onApply(
                            SearchFilters(
                                rating = ratings,
                                warnings = warnings,
                                categories = categories,
                                fandom = fandom,
                                relationship = relationship,
                                character = character,
                                otherTags = otherTags,
                                minWords = minWords.toIntOrNull(),
                                maxWords = maxWords.toIntOrNull(),
                                singleChapterOnly = singleChapter,
                                completeOnly = completeOnly,
                                excludeCrossovers = noCrossovers,
                            ),
                        )
                    },
                    enabled = !invalidRange,
                ) { Text("Apply") }
            },
        ) {
            Text("Rating", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SearchFilters.RATING_IDS.forEach { (id, label) ->
                    FilterChip(
                        selected = id in ratings,
                        onClick = { ratings = if (id in ratings) ratings - id else ratings + id },
                        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }

            Spacer(Modifier.height(Spacing.md))
            Text("Archive warnings", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SearchFilters.WARNING_IDS.forEach { (id, label) ->
                    FilterChip(
                        selected = id in warnings,
                        onClick = { warnings = if (id in warnings) warnings - id else warnings + id },
                        label = { Text(label) },
                    )
                }
            }

            Spacer(Modifier.height(Spacing.md))
            Text("Category", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SearchFilters.CATEGORY_IDS.forEach { (id, label) ->
                    FilterChip(
                        selected = id in categories,
                        onClick = {
                            categories = if (id in categories) categories - id else categories + id
                        },
                        label = { Text(label) },
                    )
                }
            }

            Spacer(Modifier.height(Spacing.md))
            FilterField(
                label = "Fandom",
                value = fandom,
                supporting = "Type a name, tap a suggestion, then save it as a chip.",
            ) { typed ->
                fandom = typed
                if (!typed.equals(savedFandom, ignoreCase = true)) savedFandom = null
                onFandomInput(typed)
            }

            // AO3's own tag autocomplete, so nobody has to spell a fandom exactly.
            if (suggestions.isNotEmpty() && suggestions.none { it.equals(fandom, ignoreCase = true) }) {
                Column(modifier = Modifier.padding(bottom = Spacing.xs)) {
                    suggestions.take(6).forEach { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    fandom = suggestion
                                    savedFandom = null
                                    onFandomInput(suggestion)
                                }
                                .padding(vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Ao3Icons.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(Spacing.sm))
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            val saved = savedFandom?.equals(fandom.trim(), ignoreCase = true) == true
            TextButton(
                onClick = {
                    onSaveFandom(fandom)
                    savedFandom = fandom.trim()
                },
                enabled = fandom.isNotBlank() && !saved,
            ) {
                Icon(
                    imageVector = if (saved) Ao3Icons.Check else Ao3Icons.BookmarkOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(if (saved) "Saved to your fandoms" else "Save fandom")
            }

            FilterField("Relationship", relationship) { relationship = it }
            FilterField("Character", character) { character = it }
            FilterField("Other tags", otherTags) { otherTags = it }

            Spacer(Modifier.height(Spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                OutlinedTextField(
                    value = minWords,
                    onValueChange = { minWords = it.filter(Char::isDigit) },
                    label = { Text("Min words") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = invalidRange,
                )
                OutlinedTextField(
                    value = maxWords,
                    onValueChange = { maxWords = it.filter(Char::isDigit) },
                    label = { Text("Max words") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = invalidRange,
                )
            }
            if (invalidRange) {
                Text(
                    text = "Minimum words must be less than maximum.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }

            Spacer(Modifier.height(Spacing.md))
            ToggleRow("Complete works only", completeOnly) { completeOnly = it }
            ToggleRow("Single chapter only", singleChapter) { singleChapter = it }
            ToggleRow("Exclude crossovers", noCrossovers) { noCrossovers = it }
        }
    }
}

@Composable
private fun FilterField(
    label: String,
    value: String,
    supporting: String? = null,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        supportingText = if (supporting != null) {
            { Text(supporting, style = MaterialTheme.typography.bodySmall) }
        } else {
            null
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.sm),
        singleLine = true,
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortSheet(
    current: SortOption,
    onDismiss: () -> Unit,
    onSelect: (SortOption) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        SheetLayout(title = "Sort by") {
            SortOption.entries.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = option == current,
                            onClick = { onSelect(option) },
                            role = Role.RadioButton,
                        )
                        .padding(vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = option == current, onClick = null)
                    Spacer(Modifier.width(Spacing.md))
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    if (option == current) {
                        Icon(
                            imageVector = Ao3Icons.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
            }
        }
    }
}
