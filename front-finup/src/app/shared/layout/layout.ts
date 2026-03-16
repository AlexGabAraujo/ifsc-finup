import { Component } from "@angular/core";
import { Header } from "../components/components_layout/header/header";
import { Footer } from "../components/components_layout/footer/footer";
import { Sidebar } from "../components/components_layout/sidebar/sidebar";
import { RouterOutlet } from "@angular/router";

@Component({
    selector: 'app-layout',
    standalone: true,
    imports: [ Header, RouterOutlet, Footer, Sidebar],
    templateUrl: './layout.html',
    styleUrl: './layout.css',
})
export class Layout {

}