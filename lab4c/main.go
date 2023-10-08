package main

import (
	"fmt"
	"sync"
)

// Структура для представлення рейсу
type BusRoute struct {
	From  string
	To    string
	Price int
}

// Структура для представлення графу автобусних рейсів та міст
type BusGraph struct {
	Routes map[string]map[string]BusRoute
	Mutex  sync.RWMutex
}

// Функція для зміни ціни квитка на певному рейсі
func (bg *BusGraph) ChangeTicketPrice(from, to string, newPrice int) {
	bg.Mutex.Lock()
	defer bg.Mutex.Unlock()

	route, exists := bg.Routes[from][to]
	if exists {
		route.Price = newPrice
		bg.Routes[from][to] = route
		bg.Routes[to][from] = BusRoute{To: from, From: to, Price: newPrice}
	}
}

// Функція для видалення та додавання рейсів між містами
func (bg *BusGraph) ModifyRoutes(from, to string, newRoute BusRoute) {
	bg.Mutex.Lock()
	defer bg.Mutex.Unlock()

	if _, exists := bg.Routes[from]; !exists {
		bg.Routes[from] = make(map[string]BusRoute)
	}
	if _, exists := bg.Routes[to]; !exists {
		bg.Routes[to] = make(map[string]BusRoute)
	}

	bg.Routes[from][to] = newRoute
	bg.Routes[to][from] = BusRoute{To: from, From: to, Price: newRoute.Price}
}

// Функція для видалення старих міст і додавання нових
func (bg *BusGraph) ModifyCities(oldCity, newCity string) {
	bg.Mutex.Lock()
	defer bg.Mutex.Unlock()

	delete(bg.Routes, oldCity)

	bg.Routes[newCity] = make(map[string]BusRoute)

	for city, routes := range bg.Routes {
		for _, route := range routes {
			if route.From == oldCity || route.To == oldCity {
				delete(bg.Routes[city], oldCity)
				newRoute := BusRoute{
					From:  city,
					To:    newCity,
					Price: route.Price,
				}
				bg.Routes[city][newCity] = newRoute
				bg.Routes[newCity][city] = BusRoute{To: city, From: newCity, Price: newRoute.Price}
			}
		}
	}
}

func (bg *BusGraph) FindRoutePrice(from, to string) (int, bool) {
	bg.Mutex.RLock()
	defer bg.Mutex.RUnlock()

	visited := make(map[string]bool)
	return bg.findRoutePriceDFS(from, to, visited)
}

func (bg *BusGraph) findRoutePriceDFS(current, to string, visited map[string]bool) (int, bool) {
	if current == to {
		return 0, true
	}

	visited[current] = true
	minPrice := -1
	found := false

	for city, route := range bg.Routes[current] {
		if !visited[city] {
			if price, ok := bg.findRoutePriceDFS(city, to, visited); ok {
				totalPrice := route.Price + price
				if minPrice == -1 || totalPrice < minPrice {
					minPrice = totalPrice
					found = true
				}
			}
		}
	}

	visited[current] = false
	return minPrice, found
}

func main() {
	busGraph := &BusGraph{
		Routes: make(map[string]map[string]BusRoute),
	}

	// Додаємо рейси та міста
	busGraph.ModifyRoutes("A", "B", BusRoute{From: "A", To: "B", Price: 50})
	busGraph.ModifyRoutes("B", "C", BusRoute{From: "B", To: "C", Price: 40})
	busGraph.ModifyRoutes("C", "D", BusRoute{From: "C", To: "D", Price: 30})

	// Змінюємо ціну квитка
	busGraph.ChangeTicketPrice("A", "B", 60)

	// Знаходимо шлях та ціну поїздки
	price, found := busGraph.FindRoutePrice("A", "D")
	if found {
		fmt.Printf("Ціна поїздки з A до D: %d\n", price)
	} else {
		fmt.Println("Шлях не знайдено")
	}

	// Додаємо новий рейс та видаляємо старий рейс
	busGraph.ModifyRoutes("A", "C", BusRoute{From: "A", To: "C", Price: 100})
	// Видаляємо старе місто та додаємо нове
	busGraph.ModifyCities("E", "")
	busGraph.ModifyCities("G", "")

	// Знаходимо шлях та ціну поїздки між деякими містами
	price, found = busGraph.FindRoutePrice("A", "C")
	if found {
		fmt.Printf("Ціна поїздки з A до C: %d\n", price)
	} else {
		fmt.Println("Шлях не знайдено")
	}

	price, found = busGraph.FindRoutePrice("B", "A")
	if found {
		fmt.Printf("Ціна поїздки з A до B: %d\n", price)
	} else {
		fmt.Println("Шлях не знайдено")
	}
}
