package com.condosuper.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.condosuper.app.data.models.JobPhoto
import com.condosuper.app.managers.FirebaseManager
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PhotosTabView() {
    var selectedView by remember { mutableStateOf(0) } // 0 = Feed, 1 = Gallery
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Photos") },
                actions = {
                    IconButton(onClick = { /* Navigate to AddPhotoView */ }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Photo")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Segmented Control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedView == 0,
                    onClick = { selectedView = 0 },
                    label = { Text("Feed") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedView == 1,
                    onClick = { selectedView = 1 },
                    label = { Text("Gallery") },
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Content
            when (selectedView) {
                0 -> PhotoFeedView()
                1 -> PhotoGalleryView()
            }
        }
    }
}

@Composable
fun PhotoFeedView() {
    val firebaseManager = FirebaseManager.getInstance()
    val photos by firebaseManager.photos.collectAsState()
    val currentEmployee by firebaseManager.currentEmployee.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var selectedMonth by remember { mutableStateOf<String?>(null) }
    var filterByCurrentSite by remember { mutableStateOf(false) }
    
    val filteredPhotos = remember(photos, searchQuery, selectedTag, selectedMonth, filterByCurrentSite, currentEmployee) {
        photos.filter { photo ->
            val matchesSearch = searchQuery.isBlank() || 
                photo.comment.contains(searchQuery, ignoreCase = true) ||
                photo.tags.any { it.contains(searchQuery, ignoreCase = true) }
            val matchesTag = selectedTag == null || photo.tags.contains(selectedTag)
            val matchesMonth = selectedMonth == null || 
                SimpleDateFormat("MMM yyyy", Locale.getDefault())
                    .format(Date(photo.date)) == selectedMonth
            val matchesSite = !filterByCurrentSite || photo.siteId == currentEmployee?.id
            
            matchesSearch && matchesTag && matchesMonth && matchesSite
        }
        .groupBy { it.sessionId }
        .values
        .map { it.sortedByDescending { p -> p.date } }
        .sortedByDescending { it.firstOrNull()?.date ?: 0L }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Search and Filters
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search photos...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterByCurrentSite,
                    onClick = { filterByCurrentSite = !filterByCurrentSite },
                    label = { Text("My Site") }
                )
            }
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(filteredPhotos) { sessionPhotos ->
                PhotoSessionCard(sessionPhotos)
            }
        }
    }
}

@Composable
fun PhotoSessionCard(photos: List<JobPhoto>) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    val firstPhoto = photos.firstOrNull() ?: return
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = dateFormat.format(Date(firstPhoto.date)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (firstPhoto.comment.isNotBlank()) {
                Text(
                    text = firstPhoto.comment,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Photo Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.height((photos.size / 3 + 1) * 100.dp)
            ) {
                items(photos) { photo ->
                    AsyncImage(
                        model = photo.imageURL ?: photo.videoURL,
                        contentDescription = null,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            // Tags
            if (firstPhoto.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    firstPhoto.tags.forEach { tag ->
                        AssistChip(
                            onClick = { },
                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoGalleryView() {
    val firebaseManager = FirebaseManager.getInstance()
    val photos by firebaseManager.photos.collectAsState()
    val currentEmployee by firebaseManager.currentEmployee.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var filterByCurrentSite by remember { mutableStateOf(false) }
    
    val filteredPhotos = remember(photos, searchQuery, selectedTag, filterByCurrentSite, currentEmployee) {
        photos.filter { photo ->
            val matchesSearch = searchQuery.isBlank() || 
                photo.comment.contains(searchQuery, ignoreCase = true) ||
                photo.tags.any { it.contains(searchQuery, ignoreCase = true) }
            val matchesTag = selectedTag == null || photo.tags.contains(selectedTag)
            val matchesSite = !filterByCurrentSite || photo.siteId == currentEmployee?.id
            
            matchesSearch && matchesTag && matchesSite
        }
        .sortedByDescending { it.date }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Search and Filters
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search photos...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterByCurrentSite,
                    onClick = { filterByCurrentSite = !filterByCurrentSite },
                    label = { Text("My Site") }
                )
            }
        }
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredPhotos) { photo ->
                AsyncImage(
                    model = photo.imageURL ?: photo.videoURL,
                    contentDescription = null,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

